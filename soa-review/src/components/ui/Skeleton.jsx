import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { cn } from '@/utils/cn';
export function Skeleton({ className }) {
    return _jsx("div", { className: cn('skeleton rounded-lg', className) });
}
export function SkeletonCard() {
    return (_jsxs("div", { className: "p-5 rounded-xl border", style: { background: 'var(--surface)', borderColor: 'var(--border)' }, children: [_jsxs("div", { className: "flex items-start gap-3 mb-4", children: [_jsx(Skeleton, { className: "w-10 h-10 rounded-lg flex-shrink-0" }), _jsxs("div", { className: "flex-1 space-y-2", children: [_jsx(Skeleton, { className: "h-4 w-3/4" }), _jsx(Skeleton, { className: "h-3 w-1/2" })] })] }), _jsx(Skeleton, { className: "h-3 w-full mb-2" }), _jsx(Skeleton, { className: "h-3 w-5/6 mb-4" }), _jsxs("div", { className: "flex gap-2", children: [_jsx(Skeleton, { className: "h-6 w-16 rounded-full" }), _jsx(Skeleton, { className: "h-6 w-20 rounded-full" }), _jsx(Skeleton, { className: "h-6 w-14 rounded-full" })] })] }));
}
export function SkeletonRow() {
    return (_jsxs("div", { className: "flex items-center gap-4 py-3", children: [_jsx(Skeleton, { className: "w-8 h-8 rounded-full flex-shrink-0" }), _jsxs("div", { className: "flex-1 space-y-1.5", children: [_jsx(Skeleton, { className: "h-3.5 w-1/3" }), _jsx(Skeleton, { className: "h-3 w-1/4" })] }), _jsx(Skeleton, { className: "h-6 w-20 rounded-full" }), _jsx(Skeleton, { className: "h-8 w-8 rounded-lg" })] }));
}
export function SkeletonStats() {
    return (_jsx("div", { className: "grid grid-cols-2 lg:grid-cols-4 gap-4", children: Array.from({ length: 4 }).map((_, i) => (_jsxs("div", { className: "p-5 rounded-xl border", style: { background: 'var(--surface)', borderColor: 'var(--border)' }, children: [_jsx(Skeleton, { className: "h-3 w-1/2 mb-3" }), _jsx(Skeleton, { className: "h-8 w-3/4 mb-1" }), _jsx(Skeleton, { className: "h-3 w-1/3" })] }, i))) }));
}
