import { jsx as _jsx } from "react/jsx-runtime";
import { createContext, useContext, useState } from 'react';
import { callApi, normalizeUser } from '@/utils/api';
const AuthContext = createContext(undefined);
export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => {
        const stored = sessionStorage.getItem('ts-user');
        return stored ? JSON.parse(stored) : null;
    });
    const [isLoading, setIsLoading] = useState(false);
    async function login(email, password) {
        setIsLoading(true);
        try {
            const backendUser = await callApi('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ email, password })
            });
            const normalizedUser = normalizeUser(backendUser);
            sessionStorage.setItem('ts-user', JSON.stringify(normalizedUser));
            sessionStorage.setItem('ts-token', backendUser.token);
            setUser(normalizedUser);
        }
        finally {
            setIsLoading(false);
        }
    }
    async function register(name, email, password, role) {
        setIsLoading(true);
        try {
            const backendResponse = await callApi('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify({ name, email, password, role })
            });
            const newUser = { ...normalizeUser(backendResponse), verified: false, onboarded: false };
            sessionStorage.setItem('ts-user', JSON.stringify(newUser));
            sessionStorage.setItem('ts-token', backendResponse.token || '');
            setUser(newUser);
            await callApi('/api/auth/request-otp', {
                method: 'POST',
                body: JSON.stringify({ email })
            });
        }
        finally {
            setIsLoading(false);
        }
    }
    async function requestOtp(email) {
        setIsLoading(true);
        try {
            await callApi('/api/auth/request-otp', {
                method: 'POST',
                body: JSON.stringify({ email }),
            });
        }
        finally {
            setIsLoading(false);
        }
    }
    async function verifyOtp(email, otp) {
        setIsLoading(true);
        try {
            const response = await callApi('/api/auth/verify-otp', {
                method: 'POST',
                body: JSON.stringify({ email, otp }),
            });
            const normalizedUser = normalizeUser(response);
            sessionStorage.setItem('ts-user', JSON.stringify(normalizedUser));
            sessionStorage.setItem('ts-token', response.token || '');
            setUser(normalizedUser);
        }
        finally {
            setIsLoading(false);
        }
    }
    function logout() {
        sessionStorage.removeItem('ts-user');
        sessionStorage.removeItem('ts-token');
        setUser(null);
    }
    return (_jsx(AuthContext.Provider, { value: { user, login, register, requestOtp, verifyOtp, logout, isLoading }, children: children }));
}
export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx)
        throw new Error('useAuth must be used within AuthProvider');
    return ctx;
}
