"use client";

import { ChangeEvent, DragEvent, useState } from "react";

interface CsvImportError {
  row: number;
  error: string;
}

interface CsvImportResult {
  totalRecords: number;
  successfulRecords: number;
  failedRecords: number;
  errors: CsvImportError[];
}

export default function Home() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<CsvImportResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState("");

  const selectFile = (selectedFile: File | null) => {
    setError("");
    setResult(null);

    if (!selectedFile) {
      return;
    }

    if (!selectedFile.name.toLowerCase().endsWith(".csv")) {
      setError("Please select a CSV file.");
      return;
    }

    setFile(selectedFile);
  };

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    selectFile(event.target.files?.[0] ?? null);
  };

  const handleDragOver = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragging(true);
  };

  const handleDragLeave = () => {
    setDragging(false);
  };

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setDragging(false);

    const droppedFile = event.dataTransfer.files?.[0] ?? null;
    selectFile(droppedFile);
  };

  const uploadFile = async () => {
    if (!file) {
      setError("Please select a CSV file first.");
      return;
    }

    setLoading(true);
    setError("");
    setResult(null);

    try {
      const formData = new FormData();
      formData.append("file", file);

      const response = await fetch(
        "http://localhost:8080/api/v1/ingest/csv",
        {
          method: "POST",
          body: formData,
        }
      );

      if (!response.ok) {
        throw new Error("Failed to upload CSV file.");
      }

      const data: CsvImportResult = await response.json();

      setResult(data);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Something went wrong while uploading the file."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-slate-950 text-white">
      <div className="mx-auto max-w-5xl px-6 py-12">

        {/* Header */}
        <div className="mb-10">
          <p className="mb-2 text-sm font-medium text-blue-400">
            TRANSACTION INGESTION
          </p>

          <h1 className="text-4xl font-bold tracking-tight">
            Transaction Import
          </h1>

          <p className="mt-3 max-w-2xl text-slate-400">
            Upload a transaction CSV file and process the records through the
            ingestion pipeline.
          </p>
        </div>

        {/* Upload Card */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900 p-6 shadow-xl">

          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            className={`rounded-xl border-2 border-dashed p-12 text-center transition ${
              dragging
                ? "border-blue-500 bg-blue-500/10"
                : "border-slate-700 hover:border-slate-500"
            }`}
          >
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-slate-800 text-2xl">
              📄
            </div>

            <h2 className="text-lg font-semibold">
              Drop your CSV file here
            </h2>

            <p className="mt-2 text-sm text-slate-400">
              or select a file from your computer
            </p>

            <label className="mt-6 inline-block cursor-pointer rounded-lg bg-slate-800 px-5 py-2.5 text-sm font-medium transition hover:bg-slate-700">
              Browse Files

              <input
                type="file"
                accept=".csv"
                onChange={handleFileChange}
                className="hidden"
              />
            </label>
          </div>

          {/* Selected File */}
          {file && (
            <div className="mt-5 flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950 px-4 py-3">
              <div>
                <p className="text-sm font-medium">
                  {file.name}
                </p>

                <p className="mt-1 text-xs text-slate-500">
                  {(file.size / 1024).toFixed(1)} KB
                </p>
              </div>

              <button
                onClick={() => setFile(null)}
                className="text-sm text-slate-400 hover:text-white"
              >
                Remove
              </button>
            </div>
          )}

          {/* Error */}
          {error && (
            <div className="mt-5 rounded-lg border border-red-900 bg-red-950/40 px-4 py-3 text-sm text-red-300">
              {error}
            </div>
          )}

          {/* Upload Button */}
          <button
            onClick={uploadFile}
            disabled={!file || loading}
            className="mt-6 w-full rounded-lg bg-blue-600 px-5 py-3 text-sm font-semibold transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {loading ? "Processing..." : "Upload Transactions"}
          </button>
        </div>

        {/* Result */}
        {result && (
          <div className="mt-8">

            <div className="mb-5">
              <h2 className="text-xl font-semibold">
                Import Summary
              </h2>

              <p className="mt-1 text-sm text-slate-400">
                CSV processing result
              </p>
            </div>

            {/* Statistics */}
            <div className="grid gap-4 sm:grid-cols-3">

              <div className="rounded-xl border border-slate-800 bg-slate-900 p-5">
                <p className="text-sm text-slate-400">
                  Total Records
                </p>

                <p className="mt-2 text-3xl font-bold">
                  {result.totalRecords}
                </p>
              </div>

              <div className="rounded-xl border border-emerald-900/50 bg-emerald-950/20 p-5">
                <p className="text-sm text-emerald-400">
                  Successful
                </p>

                <p className="mt-2 text-3xl font-bold text-emerald-400">
                  {result.successfulRecords}
                </p>
              </div>

              <div className="rounded-xl border border-red-900/50 bg-red-950/20 p-5">
                <p className="text-sm text-red-400">
                  Failed
                </p>

                <p className="mt-2 text-3xl font-bold text-red-400">
                  {result.failedRecords}
                </p>
              </div>

            </div>

            {/* Errors */}
            {result.errors.length > 0 && (
              <div className="mt-6 rounded-xl border border-slate-800 bg-slate-900">

                <div className="border-b border-slate-800 px-5 py-4">
                  <h3 className="font-semibold">
                    Import Errors
                  </h3>
                </div>

                <div className="divide-y divide-slate-800">
                  {result.errors.map((item, index) => (
                    <div
                      key={index}
                      className="flex gap-5 px-5 py-4"
                    >
                      <span className="shrink-0 text-sm font-medium text-red-400">
                        Row {item.row}
                      </span>

                      <span className="text-sm text-slate-300">
                        {item.error}
                      </span>
                    </div>
                  ))}
                </div>

              </div>
            )}

            {/* Success Message */}
            {result.errors.length === 0 && (
              <div className="mt-6 rounded-xl border border-emerald-900/50 bg-emerald-950/20 px-5 py-4">
                <p className="text-sm font-medium text-emerald-400">
                  ✓ All CSV records were successfully published for processing.
                </p>
              </div>
            )}

          </div>
        )}

      </div>
    </main>
  );
}