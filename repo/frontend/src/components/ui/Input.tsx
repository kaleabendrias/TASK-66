import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

const Input: React.FC<InputProps> = ({ label, error, className = '', id, ...props }) => {
  const inputId = id || props.name;
  return (
    <div className="form-group">
      {label && <label htmlFor={inputId} className="form-label">{label}</label>}
      <input id={inputId} className={`form-input ${error ? 'input-error' : ''} ${className}`} {...props} />
      {error && <span className="form-error">{error}</span>}
    </div>
  );
};

export default Input;
