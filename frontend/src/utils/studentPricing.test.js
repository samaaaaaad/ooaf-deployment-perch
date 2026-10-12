import { describe, expect, test } from 'vitest';

import { discounted, hasUniversityEmail, isStudent, money } from './studentPricing';

describe('studentPricing', () => {
  test('only an admin-verified user counts as a student', () => {
    expect(isStudent({ email: 'a@uni.edu.au', studentEligible: true })).toBe(true);
    expect(isStudent({ email: 'a@uni.edu.au', studentEligible: false })).toBe(false);
    expect(isStudent({ email: 'a@uni.edu.au' })).toBe(false);
    expect(isStudent(null)).toBe(false);
  });

  test('detects a university email so the user can apply', () => {
    expect(hasUniversityEmail({ email: 'sam@student.unsw.edu.au' })).toBe(true);
    expect(hasUniversityEmail({ email: 'sam@mit.edu' })).toBe(true);
    expect(hasUniversityEmail({ email: 'sam@gmail.com' })).toBe(false);
    expect(hasUniversityEmail({ email: 'sam@gmail.com', universityEmail: true })).toBe(true);
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
