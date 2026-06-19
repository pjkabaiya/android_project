import { Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import DriverDashboard from './pages/DriverDashboard';
import PassengerDashboard from './pages/PassengerDashboard';
import MapView from './pages/MapView';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/driver" element={<DriverDashboard />} />
      <Route path="/passenger" element={<PassengerDashboard />} />
      <Route path="/map/:tripId" element={<MapView />} />
    </Routes>
  );
}
