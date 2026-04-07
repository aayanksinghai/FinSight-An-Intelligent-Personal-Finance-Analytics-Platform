import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

DB_URL = os.getenv("DB_URL", "postgresql://finsight:finsight@localhost:5432/finsight_user")
# We use the db driver psycopg2, so ensure the url is correctly prefixed
if DB_URL.startswith("jdbc:postgresql://"):
    DB_URL = DB_URL.replace("jdbc:postgresql://", "postgresql://")

engine = create_engine(DB_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
