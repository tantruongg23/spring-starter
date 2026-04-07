-- Enable the pgcrypto extension if not already enabled (for UUID if needed)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create the ENUM types if they do not exist
DO $$ BEGIN
    CREATE TYPE order_status AS ENUM ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('ADMIN', 'USER');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Alter the tables to use the ENUM types instead of VARCHAR
-- The USING clause is crucial for converting existing VARCHAR data into the ENUM
ALTER TABLE orders 
  ALTER COLUMN status TYPE order_status 
  USING status::order_status;

ALTER TABLE users 
  ALTER COLUMN role TYPE user_role 
  USING role::user_role;
