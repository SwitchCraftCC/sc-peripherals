local startTColor = term.getTextColor()

local function printUsage()
  local programName = arg[0] or fs.getName(shell.getRunningProgram())
  
  term.setTextColor(colors.lightGray)
  term.write("Usage: ")

  term.setTextColor(colors.white)
  print(("%s <file or url> [count] [options?]"):format(programName))
  
  term.setTextColor(colors.gray)
  print("Options for .2dja: [ <index> | <from> <to> ]")
  
  term.setTextColor(colors.white)
  print(("Type '%s stop' to cancel the print task"):format(programName))
end

-- Find a printer
local printer = peripheral.find("poster_printer")
if not printer then
  error("No printer found.", 0)
end

-- Validate arguments
local tArgs = { ... }
if #tArgs < 1 or #tArgs > 4
or tArgs[1]:lower() == "help" then
  printUsage()
  return
end

-- Stop the printer by running `poster stop`
if tArgs[1] == "stop" then
  print("Stopping printer...")
  printer.stop()
  return
end

local status = printer.status()
if status == "busy" then
  error("Printer is busy.", 0)
end

-- Convert string to number and validate it.
local function getNum(str)
  local num = tonumber(str)
  if not num or math.floor(num) < 1 then
    return
  end
  return math.floor(num)
end

-- Get count.
local nCount = 1
if #tArgs > 1 then
  nCount = getNum(tArgs[2])
  if not nCount then
    printUsage()
    return
  end
end

-- Get index, if given.
local tIndex = { bAll = true, from = nil, to = nil }
if #tArgs >= 3 then
  local num1,num2 = getNum(tArgs[3]), getNum(tArgs[4])
  if not num1 or not num2 then
    printUsage()
    return
  end
  
  tIndex.from = math.min(num1,num2)
  tIndex.to = math.max(num1,num2)
  
  -- Index, not interval
  if not tIndex.to then
    tIndex.to = tIndex.from
  end
  
  if tIndex.from then
    tIndex.bAll = false
  end
end

local sFilename = tArgs[1]
-- Loads from local file or Url
local function loadPoster(sName)
  local stream

  if http and sName:match("^https?://") then
    print("Downloading...")
    stream = http.get{ url = sName, binary = true }
  else
    print("Loading...")
    sName = shell.resolve(sName)
    if not fs.exists(sName) then
      -- Try to find a .2dj or .2dja file if the extension did not got specified 
      if not sFilename:match(".2dj$") or not sFilename:match(".2dja$") then
        for _,sEnd in ipairs({".2dj", ".2dja"}) do
          local newName = sName..sEnd
          if fs.exists(newName) then
            return loadPoster(newName)
          end
        end
      end
      error(("File \'%s\' not found."):format(sName), 0)
    end
    stream = fs.open(sName, "r")
  end
  
  if not stream then
    error(("Could not open \'%s\'."):format(sName), 0)
  end

  local data = stream.readAll()
  stream.close()

  local data,err = textutils.unserialiseJSON(data)
  if type(data) ~= "table" then
    error(("\n\nCould not parse \'%s\': %s"):format(sName,err), 0)
  end
  return data
end

-- Check if given poster is valid
local function validPoster(data)
  if type(data.pixels) ~= "table" then
    return true, "Table \'pixels\' does not exist."
  elseif type(data.palette) ~= "table" then
    return true, "Table \'palette\' does not exist."
  end

  return false
end

local tPosters = {}
local data = loadPoster(sFilename)
-- Add all needed pages to stack (.2dja)
if type(data.pages) == "table" then
  if tIndex.bAll then
    tIndex.from = 1
    tIndex.to = #data.pages
  end

  for i=tIndex.from, tIndex.to do
    local poster = data.pages[i]
    local bError, sMsg = validPoster(poster)
    if bError then
      error(("Could not load page %s: %s"):format(i, sMsg), 0)
    end

    table.insert(tPosters, poster)
  end
else
  -- Add poster to stack x nCounter (.2dj)
  local bError, sMsg = validPoster(data)
  if bError then
    error(("Could not load .2dj file: %s"):format(sMsg), 0)
  end
  table.insert(tPosters, data)
end

-- Setup colors
local nHighlightColor = colors.lightGray
local nErrColor = colors.gray
local nGreenColor = colors.white

if term.isColor() then
  nHighlightColor = colors.yellow
  nErrColor = colors.red
  nGreenColor = colors.green
end



-- Print a given poster
local function commitPrint(data)
  printer.reset()

  if data.label then printer.setLabel(data.label) end
  if data.tooltip then printer.setTooltip(data.tooltip) end

  printer.blitPalette(data.palette)
  printer.blitPixels(1, 1, data.pixels)

  printer.commit(nCount)
end

-- Start printing (Finally...)
term.setTextColor(nHighlightColor)
print("\nHold Ctrl+T to stop\n")

term.setTextColor(colors.lightGray)
print("Now printing:")

local _,nY = term.getCursorPos()
local nW,_ = term.getSize()

-- Move everything up for enough space
if nY > 16 then
  term.scroll(4)
  nY = 15
end

local function printRow(sName, sValue, sOptional)
  term.setTextColor(nHighlightColor)
  term.write(sName)
  term.setTextColor(colors.white)
  term.write(sValue)
  if sOptional then
    term.setTextColor(colors.lightGray)
    term.write(sOptional)
  end
end

-- Loop to print all selected posters
local nPrinted = 0
local nMaxPages = #tPosters*nCount
local bTerminated = false

for i,data in ipairs(tPosters) do
  local nCur = 0
  if bTerminated then break end
  commitPrint(data)
  
  term.setCursorPos(1,nY)
  printRow("Name: ", (data.label or "???"):sub(1,nW-6))
  
  term.setCursorPos(1,nY+1)
  printRow("Status: ", (" %s%% : %s / %s "):format(0, nPrinted, nMaxPages), (" (x%s)       "):format(nCount))


  local cur,max = printer.getInkLevel()
  term.setCursorPos(1,nY+2)
  printRow("Ink: ", (" %s%%   "):format(math.floor(100/max*cur)))
  
  -- Status and Ink level
  while true do
    local e, p1 = os.pullEventRaw()
    
    if e == "poster_printer_complete" or e == "poster_printer_state" then
      local progress = 100-(tonumber(p1) or 0)
      
      if progress >= 100 then
        nPrinted = nPrinted+1
        nCur = nCur+1
      end

      term.setCursorPos(1,nY+1)
      printRow("Status: ", (" %s%% : %s / %s "):format(progress, nPrinted, nMaxPages), (" (x%s)       "):format(nCount))
      
      if nCur >= nCount then
        break
      end
    elseif e == "terminate" then
      printer.stop()
      bTerminated = true
      break
    end
  end
end

-- Print results
term.setCursorPos(1, nY+3)
if bTerminated then
  term.setTextColor(nErrColor)
  print(("Printed %s posters out of %s."):format(nPrinted+1, nMaxPages))
else
  term.setTextColor(nGreenColor)
  print(("Printed %s posters!"):format(nPrinted))
end
term.setTextColor(startTColor)