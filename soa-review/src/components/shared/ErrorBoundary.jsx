import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Component } from 'react';
import { RefreshCw, Home, Zap } from 'lucide-react';
export default class ErrorBoundary extends Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false };
    }
    static getDerivedStateFromError() {
        return { hasError: true };
    }
    render() {
        if (this.state.hasError) {
            return (_jsx("div", { className: "min-h-screen flex items-center justify-center px-5", style: { background: 'var(--background)' }, children: _jsxs("div", { className: "text-center max-w-md", children: [_jsx("div", { className: "w-16 h-16 rounded-2xl flex items-center justify-center mx-auto mb-6", style: { background: 'var(--error-light)', color: 'var(--error)' }, children: _jsx(Zap, { size: 28 }) }), _jsx("h1", { className: "text-2xl font-extrabold mb-3", style: { color: 'var(--text-primary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: "Something went wrong" }), _jsx("p", { className: "text-sm mb-8 leading-relaxed", style: { color: 'var(--text-secondary)' }, children: "An unexpected error occurred. Our team has been notified. Please try refreshing the page or return home." }), _jsxs("div", { className: "flex flex-col sm:flex-row gap-3 justify-center", children: [_jsxs("button", { onClick: () => this.setState({ hasError: false }), className: "flex items-center justify-center gap-2 px-6 py-3 rounded-xl text-sm font-semibold text-white transition-colors", style: { background: 'var(--accent)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: [_jsx(RefreshCw, { size: 15 }), " Try Again"] }), _jsxs("a", { href: "/", className: "flex items-center justify-center gap-2 px-6 py-3 rounded-xl border text-sm font-semibold transition-colors", style: { borderColor: 'var(--border-strong)', color: 'var(--text-secondary)', fontFamily: 'Plus Jakarta Sans, sans-serif' }, children: [_jsx(Home, { size: 15 }), " Return Home"] })] })] }) }));
        }
        return this.props.children;
    }
}
