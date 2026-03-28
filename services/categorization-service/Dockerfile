FROM python:3.11-slim

WORKDIR /app

# Install system dependencies needed for confluent-kafka compilation and runtime
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libc-dev \
    librdkafka-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Run as non-root user
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup
USER appuser

COPY --chown=appuser:appgroup app /app/app

EXPOSE 8000

ENV LOG_LEVEL=INFO

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
