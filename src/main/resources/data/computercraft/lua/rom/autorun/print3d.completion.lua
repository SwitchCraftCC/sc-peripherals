local completion = require "cc.shell.completion"
shell.setCompletionFunction("rom/programs/print3d.lua", completion.build(completion.file))
