declare module 'json-bigint' {
  type JsonBigFactoryOptions = {
    storeAsString?: boolean
  }

  type JsonBigInstance = {
    parse(value: string): unknown
    stringify(value: unknown): string
  }

  export default function JSONBigFactory(options?: JsonBigFactoryOptions): JsonBigInstance
}
