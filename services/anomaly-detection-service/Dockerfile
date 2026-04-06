# Stage 1: Build
FROM python:3.11-slim AS builder

WORKDIR /app

# Install build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    python3-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install dependencies
COPY requirements.txt .
# Use the PyTorch CPU index to avoid downloading massive CUDA binaries
RUN pip install --no-cache-dir --prefix=/install \
    -r requirements.txt \
    --extra-index-url https://download.pytorch.org/whl/cpu

# Stage 2: Runtime
FROM python:3.11-slim

WORKDIR /app

# Copy only the installed dependencies from the builder stage
COPY --from=builder /install /usr/local
COPY app/ ./app/

EXPOSE 8000

ENV PYTHONPATH=/
ENV KAFKA_BROKERS=kafka:9092

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
