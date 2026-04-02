import torch
import torch.nn as nn

class LSTMAutoencoder(nn.Module):
    def __init__(self, input_dim: int, hidden_dim: int = 64, num_layers: int = 1):
        super(LSTMAutoencoder, self).__init__()
        
        self.input_dim = input_dim
        self.hidden_dim = hidden_dim
        self.num_layers = num_layers
        
        # Encoder
        self.encoder = nn.LSTM(
            input_size=input_dim,
            hidden_size=hidden_dim,
            num_layers=num_layers,
            batch_first=True
        )
        
        # Decoder
        self.decoder = nn.LSTM(
            input_size=hidden_dim,
            hidden_size=hidden_dim,
            num_layers=num_layers,
            batch_first=True
        )
        
        # Output layer
        self.output_layer = nn.Linear(hidden_dim, input_dim)

    def forward(self, x):
        # x shape: (batch_size, seq_len, input_dim)
        
        # Encoder
        _, (hidden, _) = self.encoder(x)
        
        # We use the last hidden state to decode
        last_hidden = hidden[-1].unsqueeze(1) # (batch_size, 1, hidden_dim)
        
        # Repeat the hidden state for seq_len times
        seq_len = x.shape[1]
        decoder_input = last_hidden.repeat(1, seq_len, 1) # (batch_size, seq_len, hidden_dim)
        
        # Decoder
        decoder_output, _ = self.decoder(decoder_input)
        
        # Reconstruction
        reconstructed = self.output_layer(decoder_output)
        
        return reconstructed
