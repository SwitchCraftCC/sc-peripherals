local startColor = term.getTextColor()

local function printUsage()
  local programName = arg[0] or fs.getName(shell.getRunningProgram())
  print("Usage: ")
  print(programName .. " <file>.3dj [count]")
  print(programName .. " stop")
end

local function warn3dm()
  printError("This does not look like a 3dj file. If this is an OpenComputers 3dm file, please convert it to 3dj.\n")
  
  term.setTextColor(colors.white)
  term.write("Online converter: ")
  term.setTextColor(colors.blue)
  term.write("https://3dj.lem.sh")
  term.setTextColor(startColor)
end

-- Find a 3d printer
local printer = peripheral.find("3d_printer")
if not printer then
  error("No 3D printer found", 0)
end

-- Validate arguments
local args = { ... }
if #args < 1 or #args > 2 then
  printUsage()
  return
end

-- Stop the 3d printer by running `print3d stop`
if args[1] == "stop" then
  print("Stopping 3d printer...")
  printer.stop()
  return
end

local count = 1
if #args == 2 then
  count = tonumber(args[2])
  if not count then
    printUsage()
    return
  end
end

-- Load the 3dj file
local filename = args[1]
if filename:match(".3dm$") then
  warn3dm()
  return
end

local function load3djFile(name)
  if not fs.exists(name) then
    -- Try to find a .3dj file if the user didn't specify the extension
    if not filename:match(".3dj$") then
      local newName = name .. ".3dj"
      if fs.exists(newName) then
        return load3djFile(newName)
      end
    end

    error("File not found: ", 0)
  end

  local f = fs.open(name, "r")
  if not f then
    error("Could not open " .. name, 0)
  end
  local data = f.readAll()
  f.close()

  local data, err = textutils.unserialiseJSON(data)
  if data == nil then
    warn3dm()
    error("\n\nCould not parse " .. name .. ": " .. err, 0)
  end
  return data
end

local printData = load3djFile(filename)
if not printData.shapesOff or not printData.shapesOn then
  error("Invalid print data - missing shapesOff and shapesOn")
end

-- Output some basic print metadata (models often have credit in tooltips)
term.write("Printing: ")
term.setTextColor(colors.lightGray)
print(printData.label or filename)

if printData.tooltip then
  term.write(printData.tooltip)
end

print("")

local function parseTint(tint)
  if type(tint) == "string" then
    return tonumber(tint, 16)
  elseif type(tint) == "number" then
    return tint
  else
    return 0xFFFFFF
  end
end

local function addShape(shapes, shape, state)
  table.insert(shapes, {
    shape.bounds[1], shape.bounds[2], shape.bounds[3],
    shape.bounds[4], shape.bounds[5], shape.bounds[6],
    state = state,
    texture = shape.texture,
    tint = parseTint(shape.tint)
  })
end

local function commitPrint(data)
  printer.reset()

  if data.label then printer.setLabel(data.label) end
  if data.tooltip then printer.setTooltip(data.tooltip) end
  printer.setButtonMode(data.isButton or false)
  printer.setCollidable(data.collideWhenOff ~= false, data.collideWhenOn ~= false)
  printer.setRedstoneLevel(data.redstoneLevel or 0)
  printer.setLightLevel(data.lightLevel or 0)

  local shapes = {}
  for _, shape in ipairs(data.shapesOff) do
    addShape(shapes, shape, false)
  end
  for _, shape in ipairs(data.shapesOn) do
    addShape(shapes, shape, true)
  end

  printer.addShapes(shapes)

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
  local chamelium, maxChamelium = printer.getChameliumLevel()
  local ink, maxInk = printer.getInkLevel()

  term.setCursorPos(1, y - 1)
  term.clearLine()
  writePercent("Chamelium: ", chamelium, maxChamelium)
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

  if e == "3d_printer_complete" or e == "3d_printer_state" then
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
