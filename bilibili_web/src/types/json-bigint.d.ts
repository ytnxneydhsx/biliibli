declare module 'json-bigint' {
  type JsonBigInstance = {
    parse(text: string): unknown
    stringify(value: unknown): string
  }

  type JsonBigFactoryOptions = {
    storeAsString?: boolean
  }

  export default function JSONBigFactory(options?: JsonBigFactoryOptions): JsonBigInstance
}
