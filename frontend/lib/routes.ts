// Frontend route contract. Screens build paths only through these helpers.
export const routes = {
  home: () => "/",
  postDetail: (postId: number) => `/posts/${postId}`,
  postCreate: () => "/posts/new",
  postEdit: (postId: number) => `/posts/${postId}/edit`,
  devUser: () => "/dev-user",
};
