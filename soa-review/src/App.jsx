import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
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
function ProtectedRoute({ children, requiredRole }) {
    const { user } = useAuth();
    if (!user)
        return _jsx(Navigate, { to: "/login", replace: true });
    if (!user.verified) {
        return _jsx(Navigate, { to: "/verify-email", replace: true });
    }
    if (requiredRole && user.role !== requiredRole) {
        return _jsx(Navigate, { to: user.role === 'recruiter' ? '/recruiter' : '/dashboard', replace: true });
    }
    return _jsx(_Fragment, { children: children });
}
function AppRoutes() {
    const { user } = useAuth();
    const targetDashboard = user?.role === 'recruiter' ? '/recruiter' : '/dashboard';
    return (_jsxs(Routes, { children: [_jsx(Route, { path: "/", element: _jsx(Landing, {}) }), _jsx(Route, { path: "/login", element: user ? _jsx(Navigate, { to: targetDashboard, replace: true }) : _jsx(Login, {}) }), _jsx(Route, { path: "/register", element: user ? _jsx(Navigate, { to: user.verified ? targetDashboard : "/verify-email", replace: true }) : _jsx(Register, {}) }), _jsx(Route, { path: "/verify-email", element: user ? (user.verified ? _jsx(Navigate, { to: targetDashboard, replace: true }) : _jsx(VerifyEmail, {})) : _jsx(Navigate, { to: "/login", replace: true }) }), _jsx(Route, { path: "/forgot-password", element: _jsx(ForgotPassword, {}) }), _jsx(Route, { path: "/reset-password", element: _jsx(ResetPassword, {}) }), _jsx(Route, { path: "/onboarding/candidate", element: _jsx(ProtectedRoute, { children: _jsx(CandidateOnboarding, {}) }) }), _jsx(Route, { path: "/onboarding/recruiter", element: _jsx(ProtectedRoute, { children: _jsx(RecruiterOnboarding, {}) }) }), _jsx(Route, { path: "/dashboard", element: _jsx(ProtectedRoute, { requiredRole: "candidate", children: _jsx(CandidateDashboard, {}) }) }), _jsx(Route, { path: "/jobs", element: _jsx(Jobs, {}) }), _jsx(Route, { path: "/jobs/:id", element: _jsx(JobDetail, {}) }), _jsx(Route, { path: "/applications", element: _jsx(ProtectedRoute, { requiredRole: "candidate", children: _jsx(Applications, {}) }) }), _jsx(Route, { path: "/applications/:id", element: _jsx(ProtectedRoute, { requiredRole: "candidate", children: _jsx(ApplicationDetail, {}) }) }), _jsx(Route, { path: "/interviews", element: _jsx(ProtectedRoute, { requiredRole: "candidate", children: _jsx(CandidateInterviews, {}) }) }), _jsx(Route, { path: "/saved", element: _jsx(ProtectedRoute, { requiredRole: "candidate", children: _jsx(SavedJobs, {}) }) }), _jsx(Route, { path: "/profile", element: _jsx(ProtectedRoute, { requiredRole: "candidate", children: _jsx(Profile, {}) }) }), _jsx(Route, { path: "/settings", element: _jsx(ProtectedRoute, { requiredRole: "candidate", children: _jsx(CandidateSettings, {}) }) }), _jsx(Route, { path: "/recruiter", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(RecruiterDashboard, {}) }) }), _jsx(Route, { path: "/recruiter/jobs", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(RecruiterJobs, {}) }) }), _jsx(Route, { path: "/recruiter/jobs/new", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(JobCreate, {}) }) }), _jsx(Route, { path: "/recruiter/jobs/:id", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(JobCreate, {}) }) }), _jsx(Route, { path: "/recruiter/applications", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(RecruiterApplications, {}) }) }), _jsx(Route, { path: "/recruiter/candidates", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(Candidates, {}) }) }), _jsx(Route, { path: "/recruiter/candidates/:id", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(CandidateDetail, {}) }) }), _jsx(Route, { path: "/recruiter/pipeline", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(Pipeline, {}) }) }), _jsx(Route, { path: "/recruiter/interviews", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(RecruiterInterviews, {}) }) }), _jsx(Route, { path: "/recruiter/analytics", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(Analytics, {}) }) }), _jsx(Route, { path: "/recruiter/company", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(Company, {}) }) }), _jsx(Route, { path: "/recruiter/settings", element: _jsx(ProtectedRoute, { requiredRole: "recruiter", children: _jsx(RecruiterSettings, {}) }) }), _jsx(Route, { path: "*", element: _jsx(NotFound, {}) })] }));
}
export default function App() {
    return (_jsx(ErrorBoundary, { children: _jsx(ThemeProvider, { children: _jsx(BrowserRouter, { children: _jsx(AuthProvider, { children: _jsx(ToastProvider, { children: _jsx(AppRoutes, {}) }) }) }) }) }));
}
