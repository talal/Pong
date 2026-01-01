export const auth = {
  get token() {
    return sessionStorage.getItem('accessToken')
  },
  set token(v) {
    if (v) sessionStorage.setItem('accessToken', v);
    else sessionStorage.removeItem('accessToken');
  },

  // Clean logout helper
  logout() {
    this.token = null; // Triggers the setter above to remove from sessionStorage
  },

  // include JWT in any api call
  async apiFetch(input, init = {}) {
    const headers = new Headers(init.headers || {});
    const t = auth.token;
    if (t) headers.set('Authorization', 'Bearer ' + t);
    return fetch(input, { ...init, headers });
  }
}
