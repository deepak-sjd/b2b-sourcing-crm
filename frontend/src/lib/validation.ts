import { z } from "zod";

export const loginSchema = z.object({
  email: z.string().trim().min(1, "Email is required").email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});

export const registerSchema = z.object({
  fullName: z.string().trim().min(2, "Enter your full name"),
  email: z.string().trim().min(1, "Email is required").email("Enter a valid email"),
  password: z.string().min(8, "Password must be at least 8 characters"),
});

export const leadSchema = z.object({
  fullName: z.string().trim().min(2, "Name is required"),
  phone: z.string().trim().optional().or(z.literal("")),
  email: z.string().trim().email("Enter a valid email").optional().or(z.literal("")),
  city: z.string().trim().optional().or(z.literal("")),
  interest: z.string().trim().optional().or(z.literal("")),
  budget: z.union([z.number(), z.nan()]).optional(),
  source: z.string().trim().optional().or(z.literal("")),
});

export const contactSchema = z.object({
  fullName: z.string().trim().min(2, "Name is required"),
  designation: z.string().trim().optional().or(z.literal("")),
  company: z.string().trim().optional().or(z.literal("")),
  phone: z.string().trim().optional().or(z.literal("")),
  email: z.string().trim().email("Enter a valid email").optional().or(z.literal("")),
  city: z.string().trim().optional().or(z.literal("")),
  source: z.string().trim().optional().or(z.literal("")),
});

// Mirrors com.doctutorials.crm.customer.dto.AddressDto's @NotBlank/@Email constraints.
export const addressSchema = z.object({
  line1: z.string().trim().min(1, "Address line 1 is required"),
  line2: z.string().trim().optional().or(z.literal("")),
  area: z.string().trim().optional().or(z.literal("")),
  country: z.string().trim().optional().or(z.literal("")),
  state: z.string().trim().optional().or(z.literal("")),
  city: z.string().trim().optional().or(z.literal("")),
  pinCode: z.string().trim().optional().or(z.literal("")),
  phone: z.string().trim().optional().or(z.literal("")),
  email: z.string().trim().email("Enter a valid email").optional().or(z.literal("")),
  gstNo: z.string().trim().optional().or(z.literal("")),
});

// Only validates what CustomerFormDialog needs client-side feedback on before
// hitting the API - contacts and account manager are optional and enforced
// server-side (CustomerRequest / CustomerContactRequest).
export const customerSchema = z.object({
  companyName: z.string().trim().min(2, "Company name is required"),
  gstNo: z.string().trim().optional().or(z.literal("")),
  billingAddress: addressSchema,
});

// Mirrors com.doctutorials.crm.supplier.dto.SupplierRequest's @NotBlank
// constraints, plus a client-side GSTIN structure/checksum check so the form
// can flag a bad GST number before it ever reaches SupplierService/GstUtil.
export const supplierSchema = z.object({
  companyName: z.string().trim().min(2, "Company name is required"),
  category: z.string().min(1, "Pick a category"),
  gstNumber: z
    .string()
    .trim()
    .optional()
    .or(z.literal(""))
    .refine((v) => !v || /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/.test(v), {
      message: "GSTIN should look like 22AAAAA0000A1Z5",
    }),
  addressLine1: z.string().trim().min(2, "Address is required"),
  city: z.string().trim().min(1, "City is required"),
  state: z.string().trim().min(1, "State is required"),
  pincode: z.string().trim().min(1, "Pin code is required"),
  contactEmail: z.string().trim().email("Enter a valid email").optional().or(z.literal("")),
  contactPhone: z.string().trim().optional().or(z.literal("")),
});

/** Runs a zod schema and returns either the parsed data or a flat field->message
 * map, so form components don't each re-implement error extraction. */
export function validate<T extends z.ZodTypeAny>(
  schema: T,
  values: unknown,
): { success: true; data: z.infer<T> } | { success: false; errors: Record<string, string> } {
  const result = schema.safeParse(values);
  if (result.success) return { success: true, data: result.data };

  const errors: Record<string, string> = {};
  for (const issue of result.error.issues) {
    const key = issue.path.join(".") || "_form";
    if (!errors[key]) errors[key] = issue.message;
  }
  return { success: false, errors };
}