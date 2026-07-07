import type { ReactElement } from 'react';
import { Navigate } from 'react-router-dom';
import { useAppSelector } from '../store';

interface ProtectedRouteProps {
  children: ReactElement;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const token = useAppSelector((state) => state.auth.token);

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
