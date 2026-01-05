-- Function to get current tenant ID from session variable
-- This is used by RLS policies to enforce tenant isolation

CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID AS $$
DECLARE
    tenant_id TEXT;
BEGIN
    -- Get tenant ID from session variable (set per connection)
    tenant_id := current_setting('app.current_tenant_id', TRUE);

    -- Fail-fast if tenant context is not set
    -- This prevents accidental cross-tenant data access
    IF tenant_id IS NULL OR tenant_id = '' THEN
        RAISE EXCEPTION 'Tenant context not set. All queries must have tenant context.';
    END IF;

    RETURN tenant_id::UUID;
EXCEPTION
    WHEN invalid_text_representation THEN
        RAISE EXCEPTION 'Invalid tenant ID format. Must be a valid UUID.';
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

-- Grant execute permission to all database users
GRANT EXECUTE ON FUNCTION current_tenant_id() TO PUBLIC;

-- Add documentation
COMMENT ON FUNCTION current_tenant_id() IS
'Returns the current tenant ID from the session variable app.current_tenant_id.
Used by RLS policies to enforce tenant isolation.
Raises an exception if tenant context is not set.';
