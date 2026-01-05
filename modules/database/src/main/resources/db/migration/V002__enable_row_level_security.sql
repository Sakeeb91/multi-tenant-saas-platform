-- Enable Row-Level Security on tenant-scoped tables
-- This ensures all queries are automatically filtered by tenant

-- Enable RLS on resources table
ALTER TABLE resources ENABLE ROW LEVEL SECURITY;

-- Enable RLS on users table
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Force RLS for table owner (critical for security)
-- Without FORCE, the table owner bypasses RLS policies
ALTER TABLE resources FORCE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

-- Add comments for documentation
COMMENT ON TABLE resources IS 'Resources with Row-Level Security enabled for tenant isolation';
COMMENT ON TABLE users IS 'Users with Row-Level Security enabled for tenant isolation';
