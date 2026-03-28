export interface TransactionSummary {
  type: 'DEBIT' | 'CREDIT';
  total: number;
}

export interface CategorySummary {
  category: string;
  color: string;
  total: number;
}

export interface TransactionResponse {
  id: string;
  ownerEmail: string;
  occurredAt: string;
  description: string;
  merchant: string | null;
  categoryName: string | null;
  categoryColor: string | null;
  categoryIcon: string | null;
  amount: number;
  type: 'DEBIT' | 'CREDIT';
  currency: string;
  tags: string[];
  isAnomaly: boolean;
}
