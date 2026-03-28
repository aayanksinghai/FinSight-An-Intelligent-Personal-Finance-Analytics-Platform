import { apiClient } from './client';
import type { IngestionJobResponse, PageResponse } from '../types/ingestion';

/**
 * Upload a bank statement file.
 * Returns the created job immediately (status = PENDING).
 * Parsing happens asynchronously on the server.
 */
export async function uploadStatement(file: File): Promise<IngestionJobResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const { data } = await apiClient.post<IngestionJobResponse>(
    '/api/ingestion/upload',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
  return data;
}

export async function listIngestionJobs(
  page = 0,
  size = 10,
): Promise<PageResponse<IngestionJobResponse>> {
  const { data } = await apiClient.get<PageResponse<IngestionJobResponse>>(
    '/api/ingestion/jobs',
    { params: { page, size } },
  );
  return data;
}

export async function getIngestionJob(jobId: string): Promise<IngestionJobResponse> {
  const { data } = await apiClient.get<IngestionJobResponse>(
    `/api/ingestion/jobs/${jobId}`,
  );
  return data;
}
