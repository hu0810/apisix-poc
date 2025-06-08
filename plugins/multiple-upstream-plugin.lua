local core = require("apisix.core")

local rule_schema = {
    type = "object",
    properties = {
        match = {
            type = "object",
            properties = {
                vars = {
                    type = "array",
                    items = {
                        type = "object",
                        properties = {
                            var_name = { type = "string" },
                            operator = { type = "string" },
                            value = { type = "string" }
                        },
                        required = {"var_name", "operator", "value"}
                    }
                }
            },
            required = {"vars"}
        },
        url = { type = "string" },
        upstream_id = { type = "string" },
        headers = {
            type = "array",
            items = {
                type = "object",
                properties = {
                    key = { type = "string" },
                    value = { type = "string" }
                },
                required = {"key", "value"}
            }
        }
    },
    required = {"match", "upstream_id"}
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
    if not rule.match or not rule.match.vars then
        return false
    end
    for _, cond in ipairs(rule.match.vars) do
        local var_name = cond.var_name
        local op = cond.operator
        local val = cond.value
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
            if rule.headers then
                for _, h in ipairs(rule.headers) do
                    if h.key and h.value then
                        core.request.set_header(h.key, h.value)
                    end
                end
            end
            if rule.upstream_id then
                ctx.var.upstream_id = rule.upstream_id
            end
            core.log.info("multiple-upstream-plugin chose ", rule.upstream_id)
            return
        end
    end
end

return _M
