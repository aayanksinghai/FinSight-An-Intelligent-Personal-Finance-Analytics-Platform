# ML Service Optimization — Implementation Plan

The current `anomaly-detection-service` image is ~5GB because it installs the full CUDA-enabled version of PyTorch. For CPU-based inference, this is redundant and slow to pull/deploy.

## Proposed Changes

### [Component Name] Anomaly Detection Service

#### [MODIFY] [requirements.txt](file:///home/aayanksinghai/SPE/Major Project/FinSight/services/anomaly-detection-service/requirements.txt)
Switch to the CPU-only version of PyTorch.

#### [MODIFY] [Dockerfile](file:///home/aayanksinghai/SPE/Major Project/FinSight/services/anomaly-detection-service/Dockerfile)
- Use a multi-stage build to keep the final image clean.
- Explicitly point to the PyTorch CPU wheel index during installation.
- Remove build dependencies (gcc, etc.) from the final runtime image.

## Alternatives for Hosting (Free/Freemium)

| Service | Best For | Free Tier Details |
| :--- | :--- | :--- |
| **Hugging Face Spaces** | ML Demos | Free CPU tier (2 vCPU, 16GB RAM). |
| **Google Cloud Run** | Scalable APIs | Free tier for the first 180k vCPU-seconds/month. |
| **Render / Railway** | Simple Web Apps | Basic free tiers for web services. |
| **ONNX Runtime** | Extreme Size Reduction | Not a service, but a library change (replaces Torch for inference, ~200MB total). |

## Verification Plan
1. Rebuild the image: `docker build -t finsight-anomaly-optimized .`
2. Run `docker images` and compare sizes.
3. Run the container and verify that the `/predict` endpoint still works correctly on CPU.
