-- RLS Policies for tenant isolation
-- Each policy uses current_tenant_id() to filter data by tenant

-- ============================================================================
-- Resources table policies
-- ============================================================================

-- Policy for SELECT, UPDATE, DELETE operations on resources
CREATE POLICY resources_tenant_isolation ON resources
    FOR ALL
    USING (tenant_id = current_tenant_id());

-- Policy for INSERT operations (validates tenant_id matches context)
CREATE POLICY resources_tenant_insert ON resources
    FOR INSERT
    WITH CHECK (tenant_id = current_tenant_id());

-- ============================================================================
-- Users table policies
-- ============================================================================

-- Policy for SELECT, UPDATE, DELETE operations on users
CREATE POLICY users_tenant_isolation ON users
    FOR ALL
    USING (tenant_id = current_tenant_id());

-- Policy for INSERT operations (validates tenant_id matches context)
CREATE POLICY users_tenant_insert ON users
    FOR INSERT
    WITH CHECK (tenant_id = current_tenant_id());

-- ============================================================================
-- Documentation
-- ============================================================================

COMMENT ON POLICY resources_tenant_isolation ON resources IS
'Ensures resources can only be accessed by users from the same tenant.
Uses current_tenant_id() to get the tenant from session context.';

COMMENT ON POLICY users_tenant_isolation ON users IS
'Ensures users can only be accessed by users from the same tenant.
Uses current_tenant_id() to get the tenant from session context.';
