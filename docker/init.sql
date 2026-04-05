-- ============================================
-- LuxeStay Hotel Management — init.sql
-- Runs once on first PostgreSQL container start
-- ============================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- for fast LIKE searches

-- Create schema (JPA will create tables via ddl-auto=update)
-- This file seeds initial data after tables exist

-- Wait hint: JPA creates tables on app start, not here.
-- This script only adds indexes + seed data safely with IF NOT EXISTS.
