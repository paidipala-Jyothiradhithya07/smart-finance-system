import { useState, useRef } from 'react';
import { statementApi } from '../api/statementApi';
import { getErrorMessage } from '../utils/errors';

export default function StatementUpload() {
  const [files, setFiles] = useState([]);
  const [preview, setPreview] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef(null);

  const handleFileChange = (e) => {
    setFiles(Array.from(e.target.files || []));
    setError('');
    setImportResult(null);
    setPreview(null);
  };

  const handlePreview = async () => {
    if (!files.length) {
      setError('Please select at least one file.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const formData = new FormData();
      files.forEach(f => formData.append('files', f));
      const response = await statementApi.preview(formData);
      setPreview(response.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async () => {
    if (!files.length) {
      setError('Please select at least one file.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const formData = new FormData();
      files.forEach(f => formData.append('files', f));
      const response = await statementApi.upload(formData);
      setImportResult(response.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = (index) => {
    setFiles(files.filter((_, i) => i !== index));
    setPreview(null);
    setImportResult(null);
  };

  const acceptedTypes = ['.csv', '.xlsx', '.xls'];

  return (
    <div className="container py-4 animate-fade-in">
      <h1 className="h3 fw-bold mb-4">Import Bank Statement</h1>

      <div className="premium-card p-4 p-md-5">
        <div className="mb-4">
          <h2 className="h5 fw-bold mb-1">Upload your bank statement</h2>
          <p className="text-muted small mb-0">
            Supported formats: .csv, .xlsx, .xls. Maximum 5 files per upload, up to 5 MB each.
          </p>
        </div>

        {/* Drop zone */}
        <div
          className="border rounded-3 p-5 text-center bg-light mb-3"
          style={{ borderStyle: 'dashed', cursor: 'pointer' }}
          onClick={() => fileInputRef.current?.click()}
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault();
            setFiles(Array.from(e.dataTransfer.files || []));
          }}
        >
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-primary mb-3">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="17 8 12 3 7 8"></polyline>
            <line x1="12" y1="3" x2="12" y2="15"></line>
          </svg>
          <p className="fw-semibold mb-1">Click or drag files here</p>
          <p className="text-muted small mb-0">CSV or Excel files only</p>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept={acceptedTypes.join(',')}
            onChange={handleFileChange}
            style={{ display: 'none' }}
            data-testid="statement-file-input"
          />
        </div>

        {/* File list */}
        {files.length > 0 && (
          <div className="mb-3">
            <h3 className="h6 fw-bold mb-2">Selected files</h3>
            <ul className="list-group list-group-flush">
              {files.map((f, i) => (
                <li key={i} className="list-group-item d-flex justify-content-between align-items-center">
                  <span>{f.name}</span>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-danger"
                    onClick={() => handleRemove(i)}
                  >
                    Remove
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Action buttons */}
        <div className="d-flex gap-2 mb-3">
          <button
            type="button"
            className="btn btn-outline-primary"
            onClick={handlePreview}
            disabled={!files.length || loading}
            data-testid="statement-preview-btn"
          >
            {loading ? 'Checking...' : 'Preview'}
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleUpload}
            disabled={!files.length || loading}
            data-testid="statement-upload-btn"
          >
            {loading ? 'Importing...' : 'Import Now'}
          </button>
        </div>

        {error && (
          <div className="alert alert-danger" data-testid="statement-error">
            {error}
          </div>
        )}

        {preview && (
          <div className="alert alert-info" data-testid="statement-preview">
            <h3 className="h6 mb-2">Preview</h3>
            <pre className="mb-0 small">{JSON.stringify(preview, null, 2)}</pre>
          </div>
        )}

        {importResult && (
          <div className="alert alert-success" data-testid="statement-import-result">
            <h3 className="h6 mb-2">Import Complete</h3>
            <pre className="mb-0 small">{JSON.stringify(importResult, null, 2)}</pre>
          </div>
        )}
      </div>
    </div>
  );
}