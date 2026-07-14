# 🏠 Keyz Automated Listing Trust & Verification Handbook

Welcome to the centralized trust verification guide for the Keyz Real Estate Platform. This handbook explains how the platform automatically audits host identities, property listings, and banking profiles to deliver an Airbnb-grade secure marketplace without manual administrative bottlenecks.

---

## ⚡ 1. Agent Biometric Onboarding (Smile ID Integration)
To ensure that only verified, legitimate individuals can operate as agents or list properties on Keyz, the platform implements automated agent verification. This is integrated with Smile Identity's compliance registries.

*   **Selfie Liveness Verification:** When registering, the agent uploads a live selfie. The platform programmatically analyzes biometric traits to ensure liveness, preventing photo-of-photo or deepfake spoofs.
*   **Government Registry Audit:** The agent provides their National Identification Number (NIN) and Bank Verification Number (bvn). The backend securely queries government databases via Smile ID to verify matches against legal names and photographs.
*   **Instant Approval:** As soon as these programmatic liveness checks and database matches pass, the agent's account is instantly marked as approved.

---

## 📍 2. Geofenced Proof-of-Presence Verification
This prevents "phantom listings" (scammers listing properties they do not have physical access to by stealing pictures from other websites).

*   **Image Metadata Extraction:** When an agent takes and uploads a photo of the property on-site, the backend reads the raw binary image stream. Using digital metadata extractors, it parses the embedded camera GPS latitude and longitude parameters.
*   **Haversine Geodistance Check:** The backend automatically calculates the physical distance in meters between the photo's recorded coordinate and the geocoded address coordinate of the property.
*   **Zero-Friction Auto-Approval:** If the physical capture location is verified to be within twenty meters of the address location, trust points are granted, and the listing is automatically verified.

---

## ⚡ 3. Automated Utility Bill OCR Verification
If raw camera GPS coordinates are unavailable, hosts can verify listing ownership by uploading a standard utility statement (such as electric, water, or gas statements).

*   **Computer Vision Document OCR:** The platform uses Amazon Textract to parse the uploaded bill document. It automatically reads and extracts all text content, billing names, and physical addresses.
*   **Fuzzy Owner Match:** The parsed name is checked against the landlord's verified account profile, ensuring both their first and last names are fully represented in the utility bill.
*   **Fuzzy Address Match:** The address in the utility bill is tokenized and matched against the listing address. If at least sixty percent of the key street tokens, numbers, and city keywords match, it accounts for standard abbreviations (such as Rd vs Road, or St vs Street) and auto-verifies the listing.

---

## 🤖 4. Computer Vision & Duplicate Listing Detectors
To prevent scammers from copying other hosts' active property listings and advertising them at a lower price to steal deposit fees, the platform implements a visual fingerprint scanner.

*   **Perceptual Image Hashing:** When any listing photo is uploaded, the platform resizes it to a small grayscale grid, computes average pixel intensity values, and generates a unique sixty-four-bit binary image fingerprint (Average Hash).
*   **Hamming Distance Scanner:** The backend queries all active listing fingerprints. If the visual fingerprint matches an existing property photo by a different host (verified if the Hamming Distance is five bits or fewer), it detects a stolen image.
*   **Instant Security Block:** The listing is instantly flagged as a duplicate, marked as unverified, and deactivated to protect house-hunters.

---

## 💳 5. Financial Flow Micro-Deposits
To verify that the host has a valid, active financial footprint associated with their identity, Keyz enforces bank account audits.

*   **Beneficiary Name Audit:** When registering bank payout accounts, the platform checks that the payout beneficiary name contains the landlord's verified legal first and last name from their Smile ID audit, blocking payout routing to third-party bank accounts.
*   **Random Micro-Deposits:** The system generates two distinct random decimal values (representing micro-deposits) and registers them on the host bank profile.
*   **Secure Payout Confirmation:** The host inspects their bank statement and submits the two exact amounts they received. Once confirmed order-independently, the bank payout channel is activated.

---

## 👨‍💼 6. Landlord Multi-Listing & Agent Representation
The database is structured to mirror real-life real estate workflows:

*   **Multi-Listing Capabilities:** One landlord or agent can list and manage an unlimited number of properties under their verified account.
*   **Multi-Document Verification Records:** Changing property-to-verification mappings to a many-to-one layout enables hosts to submit multiple diverse verification files (such as geofenced photographs, utility statements, and land deeds) under the same listing to build cumulative trust ratings.

---

## 🛡️ 7. Programmatic Proof of Property Ownership

To verify that a host listing a house actually owns or holds access rights to that physical property without manual review, Keyz cross-references three independent validation steps to establish absolute trust:

1.  **Identity Verification:** The host completes an initial biometric selfie check and government registry lookup (NIN and BVN). This establishes their absolute, legally verified name (for example, John Doe) on the platform.
2.  **Physical Presence Geofencing:** When the host uploads listing photos, the platform reads the raw binary image stream and extracts the camera's recorded GPS latitude and longitude parameters. By comparing these parameters against the property's geocoded coordinates, the system proves the host is physically standing at the location.
3.  **Document Association OCR:** The host uploads a standard utility statement (water, gas, or electric bill) or legal deed. The computer vision engine (Amazon Textract) extracts all text:
    *   **Legal Owner Matching:** Checks that the host's legal verified first and last names exist in the utility document text.
    *   **Address Keyword Matching:** Splits the listed address into keywords and checks that at least sixty percent of these keywords exist in the utility document text, easily handling abbreviations (such as Rd vs Road, or St vs Street).
4.  **Verdict:** If all three independent steps pass, the backend establishes programmatic proof of ownership and publishes the property live!

---

## 🏛️ 8. Document Handling & Storage Architecture

All legal document files, proof statements, and photographs are processed securely:

*   **Secure AWS Storage Vault:** Document uploads are sent directly to secure AWS S3 buckets under isolated paths (such as properties/bills or properties/photos).
*   **Persistent Registry Records:** Upload URLs are stored as read-only string parameters inside verification database records.
*   **Verification History Lists:** The database maps multiple verification records to a single property listing. This enables the frontend to fetch and display a comprehensive list of all uploaded and verified files sequentially.

---

## ⚡ 9. Core Pipeline Parameters

The automated trust engines are governed by the following core system variables:

*   **Geofence Limit (Twenty Meters):** The maximum distance allowed between the camera's hardware capture coordinates and the property geocoded coordinates.
*   **OCR Address Token Matcher (Sixty Percent):** The percentage of address keywords that must match between the listed property address and the printed text on the utility bill.
*   **Duplicate Image Threshold (Five Bits):** The maximum visual difference allowed between a newly uploaded image fingerprint and existing listing photos. If the difference is five bits or fewer (out of sixty-four bits), it is flagged as a stolen/duplicate listing.
*   **Micro-Deposit Payout Limits (Between Five Cents and Ninety-Five Cents):** The range used to generate random micro-deposit verification codes for payout bank audits.
*   **Financial Confirmation Margin (One Cent):** The mathematical tolerance threshold used to verify that host micro-deposit confirmation inputs are correct.