# PostgreSQL Setup Guide for FinSight

This guide outlines the steps to set up the local PostgreSQL environment required for the FinSight microservices.

## 1. Installation
Install PostgreSQL 15+ on your system:
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

## 2. Infrastructure Setup
Create the `finsight` user and the required databases:
```bash
# Connect as postgres superuser
sudo -u postgres psql

# Create user with password
CREATE ROLE finsight WITH LOGIN PASSWORD 'finsight';
ALTER ROLE finsight CREATEDB;

# Create databases
CREATE DATABASE finsight_user;
```

## 3. Creating Schema (Critical)
The `transaction-service` is configured to use a dedicated schema called `transaction_schema` within the `finsight_user` database to avoid permission issues with the default `public` schema.

Run these commands to prepare the schema:
```bash
PGPASSWORD=finsight psql -U finsight -h localhost -d finsight_user -c "CREATE SCHEMA IF NOT EXISTS transaction_schema;"
```

## 4. Key SQL Queries for Debugging

### Check Category Mapping
Verify that transactions are being correctly categorized by the ML service:
```sql
SELECT t.id, t.raw_description, c.name as category_name, t.amount
FROM transaction_schema.transactions t
LEFT JOIN public.categories c on t.category_id = c.id
ORDER BY t.occurred_at DESC
LIMIT 20;
```

### Reset Transactions (Clean State)
If you need to re-upload statements for testing:
```sql
DELETE FROM transaction_schema.transactions;
```

### Verify Categories Table
Ensure the default categories are present (created by `user-service` migrations):
```sql
SELECT * FROM public.categories;
```

## 5. Troubleshooting Permissions
If you see `permission denied for schema public`, ensure the `finsight` user has ownership or explicit grants:
```sql
GRANT ALL ON SCHEMA public TO finsight;
```
