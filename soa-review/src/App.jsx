import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@/context/ThemeContext';
import { AuthProvider, useAuth } from '@/context/AuthContext';
import { ToastProvider } from '@/context/ToastContext';
import ErrorBoundary from '@/components/shared/ErrorBoundary';
// Pages
import Landing from '@/pages/Landing';
import Login from '@/pages/auth/Login';
import Register from '@/pages/auth/Register';
import VerifyEmail from '@/pages/auth/VerifyEmail';
import ForgotPassword from '@/pages/auth/ForgotPassword';
import ResetPassword from '@/pages/auth/ResetPassword';
import CandidateOnboarding from '@/pages/onboarding/CandidateOnboarding';
import RecruiterOnboarding from '@/pages/onboarding/RecruiterOnboarding';
import CandidateDashboard from '@/pages/candidate/Dashboard';
import Jobs from '@/pages/candidate/Jobs';
import JobDetail from '@/pages/candidate/JobDetail';
import Applications from '@/pages/candidate/Applications';
import ApplicationDetail from '@/pages/candidate/ApplicationDetail';
import CandidateInterviews from '@/pages/candidate/Interviews';
import SavedJobs from '@/pages/candidate/SavedJobs';
import Profile from '@/pages/candidate/Profile';
import CandidateSettings from '@/pages/candidate/Settings';
import RecruiterDashboard from '@/pages/recruiter/Dashboard';
import RecruiterJobs from '@/pages/recruiter/Jobs';
import JobCreate from '@/pages/recruiter/JobCreate';
import RecruiterApplications from '@/pages/recruiter/Applications';
import Candidates from '@/pages/recruiter/Candidates';
import CandidateDetail from '@/pages/recruiter/CandidateDetail';
import Pipeline from '@/pages/recruiter/Pipeline';
import RecruiterInterviews from '@/pages/recruiter/Interviews';
import Analytics from '@/pages/recruiter/Analytics';
import Company from '@/pages/recruiter/Company';
import RecruiterSettings from '@/pages/recruiter/Settings';
import NotFound from '@/pages/NotFound';

function dashboardPath(role) {
    if (role === 'recruiter') return '/recruiter';
    if (role === 'admin') return '/';
    return '/dashboard';
}

function ProtectedRoute({ children, requiredRole }) {
    const { user } = useAuth();
    if (!user) return <Navigate to="/login" replace />;
    if (!user.verified) return <Navigate to="/verify-email" replace />;
    if (requiredRole && user.role !== requiredRole) {
        return <Navigate to={dashboardPath(user.role)} replace />;
    }
    return children;
}

function AppRoutes() {
    const { user } = useAuth();
    const targetDashboard = dashboardPath(user?.role);
    return (
        <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/login" element={user ? <Navigate to={targetDashboard} replace /> : <Login />} />
            <Route path="/register" element={user ? <Navigate to={user.verified ? targetDashboard : '/verify-email'} replace /> : <Register />} />
            <Route path="/verify-email" element={user ? (user.verified ? <Navigate to={targetDashboard} replace /> : <VerifyEmail />) : <Navigate to="/login" replace />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/onboarding/candidate" element={<ProtectedRoute><CandidateOnboarding /></ProtectedRoute>} />
            <Route path="/onboarding/recruiter" element={<ProtectedRoute><RecruiterOnboarding /></ProtectedRoute>} />
            <Route path="/dashboard" element={<ProtectedRoute requiredRole="candidate"><CandidateDashboard /></ProtectedRoute>} />
            <Route path="/jobs" element={<Jobs />} />
            <Route path="/jobs/:id" element={<JobDetail />} />
            <Route path="/applications" element={<ProtectedRoute requiredRole="candidate"><Applications /></ProtectedRoute>} />
            <Route path="/applications/:id" element={<ProtectedRoute requiredRole="candidate"><ApplicationDetail /></ProtectedRoute>} />
            <Route path="/interviews" element={<ProtectedRoute requiredRole="candidate"><CandidateInterviews /></ProtectedRoute>} />
            <Route path="/saved" element={<ProtectedRoute requiredRole="candidate"><SavedJobs /></ProtectedRoute>} />
            <Route path="/profile" element={<ProtectedRoute requiredRole="candidate"><Profile /></ProtectedRoute>} />
            <Route path="/settings" element={<ProtectedRoute requiredRole="candidate"><CandidateSettings /></ProtectedRoute>} />
            <Route path="/recruiter" element={<ProtectedRoute requiredRole="recruiter"><RecruiterDashboard /></ProtectedRoute>} />
            <Route path="/recruiter/jobs" element={<ProtectedRoute requiredRole="recruiter"><RecruiterJobs /></ProtectedRoute>} />
            <Route path="/recruiter/jobs/new" element={<ProtectedRoute requiredRole="recruiter"><JobCreate /></ProtectedRoute>} />
            <Route path="/recruiter/jobs/:id" element={<ProtectedRoute requiredRole="recruiter"><JobCreate /></ProtectedRoute>} />
            <Route path="/recruiter/applications" element={<ProtectedRoute requiredRole="recruiter"><RecruiterApplications /></ProtectedRoute>} />
            <Route path="/recruiter/candidates" element={<ProtectedRoute requiredRole="recruiter"><Candidates /></ProtectedRoute>} />
            <Route path="/recruiter/candidates/:id" element={<ProtectedRoute requiredRole="recruiter"><CandidateDetail /></ProtectedRoute>} />
            <Route path="/recruiter/pipeline" element={<ProtectedRoute requiredRole="recruiter"><Pipeline /></ProtectedRoute>} />
            <Route path="/recruiter/interviews" element={<ProtectedRoute requiredRole="recruiter"><RecruiterInterviews /></ProtectedRoute>} />
            <Route path="/recruiter/analytics" element={<ProtectedRoute requiredRole="recruiter"><Analytics /></ProtectedRoute>} />
            <Route path="/recruiter/company" element={<ProtectedRoute requiredRole="recruiter"><Company /></ProtectedRoute>} />
            <Route path="/recruiter/settings" element={<ProtectedRoute requiredRole="recruiter"><RecruiterSettings /></ProtectedRoute>} />
            <Route path="*" element={<NotFound />} />
        </Routes>
    );
}

export default function App() {
    return (
        <ErrorBoundary>
            <ThemeProvider>
                <BrowserRouter>
                    <AuthProvider>
                        <ToastProvider>
                            <AppRoutes />
                        </ToastProvider>
                    </AuthProvider>
                </BrowserRouter>
            </ThemeProvider>
        </ErrorBoundary>
    );
}
