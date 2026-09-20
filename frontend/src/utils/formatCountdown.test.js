import { describe, expect, test } from 'vitest';

import { formatCountdown } from './formatCountdown';

describe('formatCountdown', () => {
    test('formats minutes and zero-padded seconds', () => {
        expect(formatCountdown(5 * 60 * 1000)).toBe('5:00');
        expect(formatCountdown(65 * 1000)).toBe('1:05');
        expect(formatCountdown(9 * 1000)).toBe('0:09');
    });

    test('rounds partial seconds up so a live hold never shows 0:00', () => {
        expect(formatCountdown(400)).toBe('0:01');
    });

    test('never goes negative', () => {
        expect(formatCountdown(0)).toBe('0:00');
        expect(formatCountdown(-5000)).toBe('0:00');
    });
});