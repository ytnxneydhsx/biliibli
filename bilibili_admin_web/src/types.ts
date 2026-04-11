export type UserLoginVO = {
  uid: string | null
  username: string
  roleCode: number
  token: string
}

export const ADMIN_ROLE_CODE = 1
