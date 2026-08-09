import { describe, expect, it } from 'vitest'
import { serializeCsv } from './csv'

describe('serializeCsv', () => {
  it('escapes quotes, commas and line breaks', () => {
    expect(serializeCsv(['标题'], [['a,b"c\nd']])).toBe('"标题"\r\n"a,b""c\nd"')
  })

  it.each(['=1+1', '+cmd', '-2+3', '@SUM(A1:A2)', '  =1+1'])(
    'prevents spreadsheet formula execution for %s',
    (value) => {
      expect(serializeCsv(['值'], [[value]])).toContain(`"'${value}"`)
    },
  )
})
