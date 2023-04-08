local startTColor = term.getTextColor()

local highlightColor = colors.lightGray
local errColor = colors.gray
local greenColor = colors.white

if term.isColor() then
  highlightColor = colors.yellow
  errColor = colors.red
  greenColor = colors.green
end

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
  error("No printer found", 0)
end

-- Validate arguments
local args = { ... }
if #args < 1 or #args > 4
or args[1]:lower() == "help" then
  printUsage()
  return
end

-- Stop the printer by running `poster stop`
if args[1] == "stop" then
  print("Stopping printer...")
  printer.stop()
  return
end

local status = printer.status()
if status == "busy" then
  error("Printer is busy", 0)
end

-- Convert string to number and validate it
local function getNum(str)
  local num = tonumber(str)
  if not num or math.floor(num) < 1 then
    return
  end
  return math.floor(num)
end

-- Get count
local count = 1
if #args > 1 then
  count = getNum(args[2])
  if not count then
    printUsage()
    return
  end
end

-- Get index, if given
local index = { all = true, from = nil, to = nil }
if #args >= 3 then
  local num1, num2 = getNum(args[3]), getNum(args[4])
  if num1 and num2 then
    -- Interval
    index.from = math.min(num1, num2)
    index.to = math.max(num1, num2)
  elseif #args > 3 then
    -- Second argument is not acceptable
    printUsage()
    return
  elseif num1 then
    -- Index, not interval
    index.from = num1
    index.to = num1
  else
    -- First argument is not acceptable
    printUsage()
    return
  end

  -- Index or interval is given, therefore we do not want to print all pages
  index.all = false
end

local filename = args[1]
-- Loads from local file or Url
local function loadPoster(name)
  local stream

  if http and name:match("^https?://") then
    print("Downloading...")
    stream = http.get{ url = name, binary = true }
  else
    print("Loading...")
    name = shell.resolve(name)
    if not fs.exists(name) then
      -- Try to find a .2dj or .2dja file if the extension did not got specified
      if not filename:match(".2dj$") or not filename:match(".2dja$") then
        local hits = 0
        local newName

        for _, sEnd in ipairs({".2dj", ".2dja"}) do
          local otherName = name..sEnd
          if fs.exists(otherName) then
            newName = otherName
            hits = hits+1
          end
        end
        if hits == 1 then
          return loadPoster(newName)
        elseif hits > 1 then
          error(("File extension for \'%s\' is not specified."):format(name), 0)
        end
      end
      error(("File \'%s\' not found."):format(name), 0)
    end
    stream = fs.open(name, "r")
  end

  if not stream then
    error(("Could not open \'%s\'"):format(name), 0)
  end

  local data = stream.readAll()
  stream.close()

  local data, err = textutils.unserialiseJSON(data)
  if type(data) ~= "table" then
    error(("\n\nCould not parse \'%s\': %s"):format(name, err), 0)
  end
  return data
end

-- Check if given poster is valid
local function isValidPoster(data)
  if type(data.pixels) ~= "table" then
    return true, "Table \'pixels\' does not exist"
  elseif type(data.palette) ~= "table" then
    return true, "Table \'palette\' does not exist"
  end

  return false
end

local posters = {}
local data = loadPoster(filename)
-- Add all needed pages to stack (.2dja)
if type(data.pages) == "table" then
  if index.all then
    index.from = 1
    index.to = #data.pages
  elseif index.to > #data.pages then
    error(("Index out of bounds (got %d, limit: %d)"):format(index.to, #data.pages), 0)
  end

  for i=index.from, index.to do
    local poster = data.pages[i]
    local hasError, msg = isValidPoster(poster)
    if hasError then
      error(("Could not load page %d: \'%s\'"):format(i, msg), 0)
    end

    table.insert(posters, poster)
  end
else
  -- Must be a single poster (.2dj)
  local hasError, msg = isValidPoster(data)
  if hasError then
    error(("Could not load .2dj file: \'%s\'"):format(msg), 0)
  end
  table.insert(posters, data)
end

-- Print a given poster
local function commitPrint(data)
  printer.reset()

  if data.label then printer.setLabel(data.label) end
  if data.tooltip then printer.setTooltip(data.tooltip) end

  printer.blitPalette(data.palette)
  printer.blitPixels(1, 1, data.pixels)

  printer.commit(count)
end

-- Start printing (Finally...)
term.setTextColor(highlightColor)
print("\nHold Ctrl+T to stop\n")

term.setTextColor(colors.lightGray)
print("Now printing:")

local _, ypos = term.getCursorPos()
local width, height = term.getSize()

-- Move everything up for enough space
if ypos > height-3 then
  term.scroll(4)
  ypos = 15
end

local function printRow(name, value, optional)
  term.setTextColor(highlightColor)
  term.write(name)
  term.setTextColor(colors.white)
  term.write(value)
  if optional then
    term.setTextColor(colors.lightGray)
    term.write(optional)
  end
end

-- Loop to print all selected posters
local printed = 0
local maxPages = #posters*count
local numDigits = math.floor(math.log10(maxPages))+1
local terminated = false

for i, data in ipairs(posters) do
  local currentPage = 0
  if terminated then break end
  commitPrint(data)

  term.setCursorPos(1, ypos)
  printRow("Name: ", (data.label or "Untitled"):sub(1, width-6))

  term.setCursorPos(1, ypos+1)
  printRow("Status: ", ("  0%% : %"..numDigits.."d / %"..numDigits.."d"):format(printed, maxPages), (" (x%d)       "):format(count))


  local cur, max = printer.getInkLevel()
  term.setCursorPos(1, ypos+2)
  printRow("Ink: ", ("%3d%%   "):format(math.floor(100/max*cur)))

  -- Status and Ink level
  while true do
    local e, p1 = os.pullEventRaw()

    if e == "poster_printer_complete" or e == "poster_printer_state" then
      local progress = 100-(tonumber(p1) or 0)

      if progress >= 100 then
        printed = printed+1
        currentPage = currentPage+1
      end

      term.setCursorPos(1, ypos+1)
      printRow("Status: ", ("%3d%% : %"..numDigits.."d / %"..numDigits.."d "):format(progress, printed, maxPages), ("(x%d)       "):format(count))
      if currentPage >= count then
        break
      end
    elseif e == "terminate" then
      printer.stop()
      terminated = true
      break
    end
  end
end

-- Print results
term.setCursorPos(1, ypos+3)
local plural = (printed ~= 1) and "posters" or "poster"
if terminated then
  plural = (printed+1 ~= 1) and "posters" or "poster"
  term.setTextColor(errColor)
  print(("Printed %d %s out of %d."):format(printed+1, plural, maxPages))
else
  term.setTextColor(greenColor)
  print(("Printed %d %s!"):format(printed, plural))
end
term.setTextColor(startTColor)
