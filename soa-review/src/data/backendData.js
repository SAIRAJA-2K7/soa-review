import { useEffect, useState } from 'react';
import { callApi } from '@/utils/api';

export function useBackendData(path, initialValue = []) {
  const [data, setData] = useState(initialValue);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  useEffect(() => {
    let active = true;
    setLoading(true);
    callApi(path)
      .then(value => active && setData(value))
      .catch(reason => active && setError(reason))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [path]);
  return { data, setData, loading, error };
}
