local completion = require "cc.shell.completion"
shell.setCompletionFunction("rom/programs/poster.lua", completion.build(completion.file))
