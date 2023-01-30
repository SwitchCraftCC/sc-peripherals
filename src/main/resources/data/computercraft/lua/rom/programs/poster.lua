local printer = peripheral.find("poster_printer")

local filename = (...)
if not filename then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <filename>")
    return
end

local image
if not fs.exists(filename) then
    if fs.exists(filename .. ".lit") then
        filename = filename .. ".lit"
    elseif fs.exists(filename .. ".nfp") then
        filename = filename .. ".nfp"
    else
        print("File not found")
        return
    end
end

printer.reset()

local shouldRemap = false
if filename:sub(-4) == ".lit" then
    local pictureTable = dofile(filename)
    image = {}
    for y = 1, pictureTable.height do
        image[y] = {}
        for x = 1, pictureTable.width do
            image[y][x] = pictureTable[(y-1) * pictureTable.width + x]
        end
    end

    for i = 1, #pictureTable.palette do
        local color = pictureTable.palette[i]
        local r = math.floor(color / 256 / 256)
        local g = math.floor(color / 256) % 256
        local b = color % 256
        printer.setPaletteColor(i, r, g, b)
    end

    if pictureTable.name then printer.setLabel(pictureTable.name) end
    if pictureTable.tooltip then printer.setTooltip(pictureTable.tooltip) end
else
    image = paintutils.loadImage(filename)
    shouldRemap = true
end

if not image then
    print("Failed to load image")
    return
end

-- colors to minecraft mapcolor id
local colorMap = {
    [colors.white] = 8,
    [colors.orange] = 15,
    [colors.magenta] = 16,
    [colors.lightBlue] = 17,
    [colors.yellow] = 18,
    [colors.lime] = 19,
    [colors.pink] = 20,
    [colors.gray] = 21,
    [colors.lightGray] = 22,
    [colors.cyan] = 23,
    [colors.purple] = 24,
    [colors.blue] = 25,
    [colors.brown] = 26,
    [colors.green] = 27,
    [colors.red] = 28,
    [colors.black] = 29,
}

for y = 1, #image do
    for x = 1, #image[y] do
        local color = image[y][x]
        if shouldRemap then
            local mapColor = colorMap[color]
            if mapColor then
                printer.setPixel(x, y, mapColor)
            end
        else
            printer.setPixel(x, y, color)
        end
    end
end

printer.commit(1)
