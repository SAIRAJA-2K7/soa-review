import { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@/context/ThemeContext';
import { AuthProvider, useAuth } from '@/context/AuthContext';
import { ToastProvider } from '@/context/ToastContext';
import ErrorBoundary from '@/components/shared/ErrorBoundary';

const Landing = lazy(() => import('@/pages/Landing'));
const Login = lazy(() => import('@/pages/auth/Login'));
const Register = lazy(() => import('@/pages/auth/Register'));
const VerifyEmail = lazy(() => import('@/pages/auth/VerifyEmail'));
const ForgotPassword = lazy(() => import('@/pages/auth/ForgotPassword'));
const ResetPassword = lazy(() => import('@/pages/auth/ResetPassword'));
const CandidateOnboarding = lazy(() => import('@/pages/onboarding/CandidateOnboarding'));
const RecruiterOnboarding = lazy(() => import('@/pages/onboarding/RecruiterOnboarding'));
const CandidateDashboard = lazy(() => import('@/pages/candidate/Dashboard'));
const Jobs = lazy(() => import('@/pages/candidate/Jobs'));
const JobDetail = lazy(() => import('@/pages/candidate/JobDetail'));
const Applications = lazy(() => import('@/pages/candidate/Applications'));
const ApplicationDetail = lazy(() => import('@/pages/candidate/ApplicationDetail'));
const CandidateInterviews = lazy(() => import('@/pages/candidate/Interviews'));
const SavedJobs = lazy(() => import('@/pages/candidate/SavedJobs'));
const Profile = lazy(() => import('@/pages/candidate/Profile'));
const CandidateSettings = lazy(() => import('@/pages/candidate/Settings'));
const RecruiterDashboard = lazy(() => import('@/pages/recruiter/Dashboard'));
const RecruiterJobs = lazy(() => import('@/pages/recruiter/Jobs'));
const JobCreate = lazy(() => import('@/pages/recruiter/JobCreate'));
const RecruiterApplications = lazy(() => import('@/pages/recruiter/Applications'));
const Candidates = lazy(() => import('@/pages/recruiter/Candidates'));
const CandidateDetail = lazy(() => import('@/pages/recruiter/CandidateDetail'));
const Pipeline = lazy(() => import('@/pages/recruiter/Pipeline'));
const RecruiterInterviews = lazy(() => import('@/pages/recruiter/Interviews'));
const Analytics = lazy(() => import('@/pages/recruiter/Analytics'));
const Company = lazy(() => import('@/pages/recruiter/Company'));
const RecruiterSettings = lazy(() => import('@/pages/recruiter/Settings'));
const NotFound = lazy(() => import('@/pages/NotFound'));

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
        <Suspense fallback={<main className="flex min-h-screen items-center justify-center text-slate-600 dark:text-slate-300" role="status">Loading TalentSync...</main>}>
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
        </Suspense>
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
