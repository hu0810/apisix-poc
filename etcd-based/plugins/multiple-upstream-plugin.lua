local core = require("apisix.core")

local rule_schema = {
    type = "object",
    properties = {
        name = { type = "string" },
        match = { type = "array" },
        upstream_id = { type = "string" },
        inject_headers = { type = "object" }
    },
    required = {"name", "match", "upstream_id"}
}

local schema = {
    type = "object",
    properties = {
        rules = { type = "array", items = rule_schema }
    },
    required = {"rules"}
}

local plugin_name = "multiple-upstream-plugin"

local _M = {
    version = 0.1,
    priority = 5060,
    name = plugin_name,
    schema = schema,
}

local function match_rule(rule, ctx)
    if not rule.match then
        return false
    end
    for _, cond in ipairs(rule.match) do
        local var_name, op, val = cond[1], cond[2], cond[3]
        local v = ctx.var[var_name]
        if op == "contains" then
            if not v or not string.find(v, val, 1, true) then
                return false
            end
        elseif op == "==" then
            if v ~= val then
                return false
            end
        end
    end
    return true
end

function _M.access(conf, ctx)
    for _, rule in ipairs(conf.rules or {}) do
        if match_rule(rule, ctx) then
            if rule.inject_headers then
                for k, v in pairs(rule.inject_headers) do
                    core.request.set_header(k, v)
                end
            end
            ctx.var.upstream_id = rule.upstream_id
            core.log.info("multiple-upstream-plugin chose ", rule.upstream_id)
            return
        end
    end
end

return _M
