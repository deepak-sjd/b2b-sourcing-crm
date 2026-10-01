/** Mirrors com.doctutorials.crm.common.ApiResponse<T> — every backend endpoint
 * returns this envelope, success or failure, so parsing is uniform app-wide. */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

/** Mirrors com.doctutorials.crm.common.PageResponse<T> (wraps Spring's Page<T>). */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
}
