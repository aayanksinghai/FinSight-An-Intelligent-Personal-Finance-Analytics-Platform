// ─── Ingestion ────────────────────────────────────────────────────────────────

export interface IngestionJobResponse {
  jobId: string;
  ownerEmail: string;
  fileName: string;
  fileSizeBytes: number;
  contentType: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  detectedBank: string | null;
  rowsParsed: number;
  rowsTotal: number;
  errorMessage: string | null;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
}

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
