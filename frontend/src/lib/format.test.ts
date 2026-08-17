import { describe, expect, it } from 'vitest'
import { titleCase } from './format'
describe('titleCase', () => {
  it('formats API enums', () => expect(titleCase('IN_PROGRESS')).toBe('In Progress'))
})
