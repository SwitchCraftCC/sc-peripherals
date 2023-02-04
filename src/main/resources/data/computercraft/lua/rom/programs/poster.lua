local startColor = term.getTextColor()

local function printUsage()
  local programName = arg[0] or fs.getName(shell.getRunningProgram())
  print("Usage: ")
  print(programName .. " <file.2dj or url> [count]")
  print(programName .. " stop")
end

-- Find a printer
local printer = peripheral.find("poster_printer")
if not printer then
  error("No printer found", 0)
end

-- Validate arguments
local args = { ... }
if #args < 1 or #args > 2 then
  printUsage()
  return
end

-- Stop the printer by running `poster stop`
if args[1] == "stop" then
  print("Stopping printer...")
  printer.stop()
  return
end

local count = 1
if #args == 2 then
  count = tonumber(args[2])
  if not count or math.floor(count) < 1 then
    printUsage()
    return
  end
  count = math.floor(count)
end

-- Load the 2dj file
local filename = args[1]

local function load2djFile(name)
  local f
  if http and name:match("^https?://") then
    print("Downloading...")
    f = http.get{ url = name, binary = true }
  else
    name = shell.resolve(name)
    if not fs.exists(name) then
      -- Try to find a .2dj file if the user didn't specify the extension
      if not filename:match(".2dj$") then
        local newName = name .. ".2dj"
        if fs.exists(newName) then
          return load2djFile(newName)
        end
      end
      error("File not found: " .. name, 0)
    end
    f = fs.open(name, "r")
  end
  
  if not f then
    error("Could not open " .. name, 0)
  end
  local data = f.readAll()
  f.close()

  local data, err = textutils.unserialiseJSON(data)
  if data == nil then
    error("\n\nCould not parse " .. name .. ": " .. err, 0)
  end
  return data
end

local printData = load2djFile(filename)
if not printData.pixels then
  error("Invalid print data - missing pixels")
end

-- Output some basic print metadata (models often have credit in tooltips)
term.write("Printing: ")
term.setTextColor(colors.lightGray)
print(printData.label or filename)

if printData.tooltip then
  term.write(printData.tooltip)
end

print("")

local function commitPrint(data)
  printer.reset()

  if data.label then printer.setLabel(data.label) end
  if data.tooltip then printer.setTooltip(data.tooltip) end

  printer.blitPalette(data.palette)
  printer.blitPixels(1, 1, data.pixels)

  printer.commit(count)
end

local function percent(n, m)
  return math.ceil(n / m * 100)
end

local function writePercent(label, n, m)
  local level = percent(n, m)

  term.setTextColor(colors.yellow)
  term.write(label)
  
  term.setTextColor(level >= 10 and colors.white or colors.red)
  term.write(level .. "%  ")
end

local remaining = count
local function showPrintStatus()
  local _, y = term.getCursorPos()
  local ink, maxInk = printer.getInkLevel()

  term.setCursorPos(1, y - 1)
  term.clearLine()
  writePercent("Ink: ", ink, maxInk)
  
  term.setCursorPos(1, y)
  term.clearLine()
  term.setTextColor(colors.yellow)
  term.write("Printing... ")
  term.setTextColor(colors.white)
  term.write(math.min((count - remaining) + 1, count) .. " / " .. count)

  term.setCursorPos(1, y)
end

-- Begin printing
local status = printer.status()
if status == "busy" then
  error("Printer is busy", 0)
end

commitPrint(printData)

term.setTextColor(colors.yellow)
print("\nHold Ctrl+T to stop\n")
showPrintStatus()

while true do
  local e, p1 = os.pullEventRaw()
  local n = math.min(math.max((count - remaining) + 1, 1), count)

  if e == "poster_printer_complete" or e == "poster_printer_state" then
    remaining = p1
    showPrintStatus()

    if remaining <= 0 then
      -- Printing complete, print the count we managed to print
      local _, y = term.getCursorPos()
      term.setCursorPos(1, y)
      term.clearLine()
      term.setTextColor(colors.green)
      print("Printed " .. n .. " item" .. (n ~= 1 and "s" or ""))

      break
    end
  elseif e == "terminate" then    
    -- Halt printing and print the count we managed to print
    printer.stop()

    local _, y = term.getCursorPos()
    term.setCursorPos(1, y)
    term.clearLine()
    term.setTextColor(colors.red)
    print("Printing terminated. Printed " .. n .. " item" .. 
      (n ~= 1 and "s" or "") .. " out of " .. count)

    break 
  end
end

term.setTextColor(startColor)
