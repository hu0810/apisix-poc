-- /usr/local/apisix/apisix/plugins/custom-auth.lua

local core = require("apisix.core")

local schema = {
    type = "object",
    properties = {
        persona_type = { type = "string" },
        user = { type = "string" }
    },
    required = { "persona_type", "user" }
}

local plugin_name = "custom-auth"

local _M = {
    version = 0.1,
    priority = 3000,
    name = plugin_name,
    schema = schema
}

function _M.check_schema(conf)
    return core.schema.check(schema, conf)
end

function _M.access(conf, ctx)
    local header_user = ctx.var.http_x_user

    if not header_user then
        return 401, { message = "Missing X-User header" }
    end

    if header_user ~= conf.user then
        return 403, { message = "Unauthorized user: " .. header_user }
    end

    core.log.info("custom-auth: access granted for ", header_user)
end

return _M
