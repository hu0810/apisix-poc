local core = require("apisix.core")

local schema = {
    type = "object",
    properties = {
        persona_type = {type = "string"},
        user = {type = "string"},
        url = {type = "string"},
        upstream_id = {type = "string"},
    },
    required = {"persona_type", "user"},
}

local plugin_name = "dynamic-upstream"

local _M = {
    version = 0.1,
    priority = 5050,
    name = plugin_name,
    schema = schema,
}

function _M.access(conf, ctx)
    core.log.info("dynamic-upstream plugin triggered")
    core.log.info("persona_type: ", conf.persona_type)
    core.log.info("user: ", conf.user)
    if conf.url then
        core.log.info("url: ", conf.url)
    end
    if conf.upstream_id then
        core.log.info("upstream_id: ", conf.upstream_id)
    end
    return
end

return _M
