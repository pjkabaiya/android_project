import { Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import Home from './pages/Home';
import Profile from './pages/Profile';
import DriverDashboard from './pages/DriverDashboard';
import PassengerDashboard from './pages/PassengerDashboard';
import MapView from './pages/MapView';

function RequireAuth({ children, role }) {
  const token = localStorage.getItem('auth_token');
  const userStr = localStorage.getItem('auth_user');
  if (!token || !userStr) return <Navigate to="/login" replace />;
  const user = JSON.parse(userStr);
  if (role && user.role !== role) {
    return <Navigate to={user.role === 'driver' ? '/driver' : '/passenger'} replace />;
  }
  return children;
}

function RedirectIfAuth({ children }) {
  const token = localStorage.getItem('auth_token');
  const userStr = localStorage.getItem('auth_user');
  if (token && userStr) {
    const user = JSON.parse(userStr);
    return <Navigate to={user.role === 'driver' ? '/driver' : '/passenger'} replace />;
  }
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<RedirectIfAuth><Home /></RedirectIfAuth>} />
      <Route path="/login" element={<RedirectIfAuth><Login /></RedirectIfAuth>} />
      <Route path="/register" element={<RedirectIfAuth><Register /></RedirectIfAuth>} />
      <Route path="/driver" element={<RequireAuth role="driver"><DriverDashboard /></RequireAuth>} />
      <Route path="/passenger" element={<RequireAuth role="passenger"><PassengerDashboard /></RequireAuth>} />
      <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
      <Route path="/map/:tripId" element={<RequireAuth><MapView /></RequireAuth>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
