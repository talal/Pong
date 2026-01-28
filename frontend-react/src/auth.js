export const auth = {
  get token() {
    return sessionStorage.getItem('accessToken')
  },
  set token(v) {
    if (v) sessionStorage.setItem('accessToken', v)
    else sessionStorage.removeItem('accessToken')
  },
  logout() {
    this.token = null
  },
}
