import { useCallback, useEffect, useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getIngestionJob, listIngestionJobs, uploadStatement } from '../api/ingestionApi';
import Notice from '../components/Notice';
import { extractApiError } from '../utils/errors';
import type { IngestionJobResponse } from '../types/ingestion';

// ── Status badge ──────────────────────────────────────────────────────────────

function StatusBadge({ status }: { status: IngestionJobResponse['status'] }) {
  const config: Record<string, string> = {
    PENDING:    'badge badge-yellow',
    PROCESSING: 'badge badge-blue animate-pulse-slow',
    COMPLETED:  'badge badge-green',
    FAILED:     'badge badge-red',
  };
  return <span className={config[status] ?? 'badge'}>{status}</span>;
}

// ── Per-row live polling for in-progress jobs ─────────────────────────────────

function LiveJobRow({ job }: { job: IngestionJobResponse }) {
  const isActive = job.status === 'PENDING' || job.status === 'PROCESSING';

  const { data: liveJob } = useQuery({
    queryKey: ['ingestion-job', job.jobId],
    queryFn: () => getIngestionJob(job.jobId),
    refetchInterval: isActive ? 2500 : false,
    initialData: job,
    enabled: isActive,
  });

  const display = liveJob ?? job;

  return (
    <tr>
      <td>
        <p className="max-w-[180px] truncate font-medium text-[#edf2ff]">{display.fileName}</p>
        <p className="text-[11px] text-muted">{display.detectedBank ?? '—'}</p>
      </td>
      <td><StatusBadge status={display.status} /></td>
      <td className="text-right tabular-nums text-sm">
        {display.status === 'COMPLETED' ? (
          <span className="text-success">{display.rowsParsed.toLocaleString()}</span>
        ) : display.status === 'PROCESSING' ? (
          <span className="flex items-center justify-end gap-1.5 text-brand">
            <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-brand" />
            Parsing…
          </span>
        ) : (
          <span className="text-muted">—</span>
        )}
      </td>
      <td className="text-right text-xs text-muted">
        {display.completedAt
          ? new Date(display.completedAt).toLocaleString()
          : display.startedAt
          ? new Date(display.startedAt).toLocaleString()
          : new Date(display.createdAt).toLocaleString()}
      </td>
      <td>
        {display.status === 'FAILED' && display.errorMessage && (
          <span className="max-w-[200px] truncate text-[11px] text-danger" title={display.errorMessage}>
            {display.errorMessage}
          </span>
        )}
      </td>
    </tr>
  );
}

// ── Drop zone ─────────────────────────────────────────────────────────────────

const ACCEPTED_TYPES = ['text/csv', 'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/pdf'];
const ACCEPTED_EXTS = ['.csv', '.xls', '.xlsx', '.pdf'];

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function UploadPage() {
  const queryClient = useQueryClient();
  const inputRef = useRef<HTMLInputElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadError, setUploadError] = useState('');
  const [uploadSuccess, setUploadSuccess] = useState('');
  const [jobsPage, setJobsPage] = useState(0);

  // Job list query
  const { data: jobsData, isLoading: jobsLoading } = useQuery({
    queryKey: ['ingestion-jobs', jobsPage],
    queryFn: () => listIngestionJobs(jobsPage, 10),
    refetchInterval: 5000,
  });

  // Upload mutation
  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadStatement(file),
    onSuccess: (job) => {
      setSelectedFile(null);
      setUploadError('');
      setUploadSuccess(
        `Upload accepted! Job ID: ${job.jobId} — parsing in background.`,
      );
      void queryClient.invalidateQueries({ queryKey: ['ingestion-jobs'] });
    },
    onError: (err) => {
      setUploadError(extractApiError(err, 'Upload failed. Please try again.'));
      setUploadSuccess('');
    },
  });

  function validateAndSelect(file: File | null) {
    if (!file) return;
    const ext = file.name.toLowerCase();
    const valid = ACCEPTED_EXTS.some((e) => ext.endsWith(e));
    if (!valid) {
      setUploadError('Unsupported file type. Please upload CSV, XLS, XLSX, or PDF.');
      return;
    }
    if (file.size > 20 * 1024 * 1024) {
      setUploadError('File exceeds the 20 MB limit.');
      return;
    }
    setUploadError('');
    setSelectedFile(file);
  }

  const onDrop = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0] ?? null;
    validateAndSelect(file);
  }, []);

  function handleUpload() {
    if (!selectedFile) return;
    uploadMutation.mutate(selectedFile);
  }

  // Auto-clear success message when user picks a new file
  useEffect(() => {
    if (selectedFile) setUploadSuccess('');
  }, [selectedFile]);

  const jobs = jobsData?.content ?? [];
  const totalPages = jobsData?.totalPages ?? 1;

  return (
    <div className="flex flex-col gap-5 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold text-[#edf2ff]">Upload Statement</h1>
        <p className="mt-0.5 text-sm text-muted">
          Upload your bank statement (CSV, XLS, XLSX, or PDF) to start ingestion.
          Rows are parsed in the background and routed to Kafka.
        </p>
      </div>

      {/* ── Drop zone ── */}
      <div className="glass-card p-6">
        <div
          id="upload-dropzone"
          role="button"
          tabIndex={0}
          className={`flex cursor-pointer flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed
            px-6 py-12 transition-all duration-200
            ${isDragging ? 'border-brand bg-brand/10' : 'border-stroke hover:border-brand/60 hover:bg-white/[0.02]'}`}
          onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
          onDragLeave={() => setIsDragging(false)}
          onDrop={onDrop}
          onClick={() => inputRef.current?.click()}
          onKeyDown={(e) => e.key === 'Enter' && inputRef.current?.click()}
        >
          <input
            ref={inputRef}
            id="file-input"
            type="file"
            accept=".csv,.xls,.xlsx,.pdf"
            className="hidden"
            onChange={(e) => validateAndSelect(e.target.files?.[0] ?? null)}
          />

          <div className="flex h-14 w-14 items-center justify-center rounded-full border border-brand/30 bg-brand/10 text-2xl">
            {isDragging ? '📥' : '📄'}
          </div>

          {selectedFile ? (
            <div className="text-center">
              <p className="font-semibold text-[#edf2ff]">{selectedFile.name}</p>
              <p className="text-sm text-muted">{formatBytes(selectedFile.size)}</p>
            </div>
          ) : (
            <div className="text-center">
              <p className="font-semibold text-[#edf2ff]">Drop your bank statement here</p>
              <p className="text-sm text-muted">or click to browse — CSV, XLS, XLSX, PDF · max 20 MB</p>
            </div>
          )}
        </div>

        {/* Notices */}
        <div className="mt-4 flex flex-col gap-3">
          {uploadError && <Notice type="error" text={uploadError} />}
          {uploadSuccess && <Notice type="success" text={uploadSuccess} />}
        </div>

        {/* Supported banks info */}
        <div className="mt-4 rounded-xl border border-stroke/50 bg-white/[0.02] px-4 py-3">
          <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-muted">Supported bank formats</p>
          <p className="text-xs text-muted">
            HDFC · ICICI · SBI · Axis · Kotak · IDFC First · Yes Bank · PNB · Canara · Union Bank · Bank of Baroda · IndusInd · RBL · Federal Bank · and most generic CSV exports
          </p>
        </div>

        <div className="mt-4 flex gap-3">
          <button
            id="upload-submit"
            className="btn-primary"
            disabled={!selectedFile || uploadMutation.isPending}
            onClick={handleUpload}
          >
            {uploadMutation.isPending ? (
              <><span className="spinner" /> Uploading…</>
            ) : (
              '↑ Upload & Parse'
            )}
          </button>

          {selectedFile && (
            <button
              className="btn-ghost"
              onClick={() => { setSelectedFile(null); setUploadError(''); }}
            >
              Clear
            </button>
          )}
        </div>
      </div>

      {/* ── Job history ── */}
      <div className="glass-card overflow-hidden">
        <div className="flex items-center justify-between border-b border-stroke px-5 py-4">
          <h2 className="text-base font-semibold text-[#edf2ff]">Upload History</h2>
          <span className="text-xs text-muted">
            {jobsData?.totalElements ?? 0} total uploads · auto-refreshes every 5 s
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>File</th>
                <th>Status</th>
                <th className="text-right">Rows parsed</th>
                <th className="text-right">Last updated</th>
                <th>Error</th>
              </tr>
            </thead>
            <tbody>
              {jobsLoading && jobs.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center">
                    <span className="spinner mx-auto block" />
                  </td>
                </tr>
              ) : jobs.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-10 text-center text-sm text-muted">
                    No uploads yet. Upload your first bank statement above.
                  </td>
                </tr>
              ) : (
                jobs.map((job) => <LiveJobRow key={job.jobId} job={job} />)
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-stroke px-5 py-3">
            <span className="text-xs text-muted">
              Page {jobsPage + 1} of {totalPages}
            </span>
            <div className="flex gap-2">
              <button
                className="btn-ghost text-xs px-3 py-1.5"
                disabled={jobsPage <= 0}
                onClick={() => setJobsPage((p) => p - 1)}
              >
                ← Prev
              </button>
              <button
                className="btn-ghost text-xs px-3 py-1.5"
                disabled={jobsPage + 1 >= totalPages}
                onClick={() => setJobsPage((p) => p + 1)}
              >
                Next →
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
