import { describe, expect, test } from 'vitest';

import { discounted, isStudent, money } from './studentPricing';

describe('studentPricing', () => {
  test('trusts the server flag when present', () => {
    expect(isStudent({ email: 'a@uni.edu.au', studentEligible: false })).toBe(false);
    expect(isStudent({ email: 'a@gmail.com', studentEligible: true })).toBe(true);
  });

  test('falls back to the university email rule for older stored sessions', () => {
    expect(isStudent({ email: 'sam@student.unsw.edu.au' })).toBe(true);
    expect(isStudent({ email: 'sam@mit.edu' })).toBe(true);
    expect(isStudent({ email: 'sam@gmail.com' })).toBe(false);
    expect(isStudent(null)).toBe(false);
  });

  test('discounts to the cent', () => {
    expect(discounted(25, 20)).toBe(20);
    expect(discounted('9.99', 15)).toBe(8.49);
    expect(discounted(25, 0)).toBe(25);
  });

  test('formats money', () => {
    expect(money(20)).toBe('$20');
    expect(money(8.5)).toBe('$8.50');
  });
});
