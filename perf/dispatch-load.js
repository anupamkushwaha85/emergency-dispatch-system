import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        ramp_dispatch: {
            executor: 'ramping-vus',
            startVUs: 5,
            stages: [
                { duration: '1m', target: 25 },
                { duration: '2m', target: 50 },
                { duration: '2m', target: 100 },
                { duration: '1m', target: 0 },
            ],
            gracefulRampDown: '30s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.02'],
        http_req_duration: ['p(95)<1200', 'p(99)<2000'],
        checks: ['rate>0.99'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

function jsonRequest(method, path, payload, token) {
    const headers = {
        'Content-Type': 'application/json',
    };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const body = payload ? JSON.stringify(payload) : null;
    return http.request(method, `${BASE_URL}${path}`, body, { headers });
}

export default function () {
    const now = Date.now();

    const createEmergencyPayload = {
        callerName: `Perf Caller ${__VU}-${__ITER}`,
        callerPhone: `99999${(__VU + __ITER) % 10000}`.padEnd(10, '0').slice(0, 10),
        latitude: 28.6139 + ((__VU % 10) * 0.001),
        longitude: 77.2090 + ((__ITER % 10) * 0.001),
        type: 'MEDICAL',
        notes: `k6 dispatch load run ${now}`,
    };

    const createResp = jsonRequest('POST', '/api/emergency/create', createEmergencyPayload);

    const createOk = check(createResp, {
        'create emergency: status 200/201': (r) => r.status === 200 || r.status === 201,
    });

    if (createOk) {
        const responseJson = createResp.json();
        const emergencyId = responseJson?.id || responseJson?.emergencyId;

        if (emergencyId) {
            const dispatchResp = jsonRequest('POST', `/api/emergency/${emergencyId}/dispatch`, null);
            check(dispatchResp, {
                'dispatch call: status 200/202/409': (r) => [200, 202, 409].includes(r.status),
            });
        }
    }

    sleep(0.2);
}
